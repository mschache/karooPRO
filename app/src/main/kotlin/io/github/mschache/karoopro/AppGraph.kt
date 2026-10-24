package io.github.mschache.karoopro

import android.app.Application
import android.content.Context
import android.util.Log
import io.github.mschache.karoopro.alerts.AlertQueue
import io.github.mschache.karoopro.coach.CoachContract
import io.github.mschache.karoopro.coach.CoachEffort
import io.github.mschache.karoopro.coach.CoachLink
import io.github.mschache.karoopro.data.Metric
import io.github.mschache.karoopro.data.PanelController
import io.github.mschache.karoopro.data.RideDataHub
import io.github.mschache.karoopro.data.RideRecorder
import io.github.mschache.karoopro.data.RideSnapshotStore
import io.github.mschache.karoopro.data.RouteTracker
import io.github.mschache.karoopro.data.SegmentMonitor
import io.github.mschache.karoopro.data.SettingsRepository
import io.github.mschache.karoopro.data.WorkoutMonitor
import io.github.mschache.karoopro.weather.WeatherRepository
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.models.DataType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

const val EXTENSION_ID = "karoopro"

/** Process-wide services, shared by the extension service, field views, tap actions and settings UI. */
class AppGraph(context: Context) {
    val app: Context = context.applicationContext
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val karoo = KarooSystemService(app)
    val settingsRepository = SettingsRepository(app, scope)
    val settings = settingsRepository.settings
    val hub = RideDataHub(karoo, scope)
    val alerts = AlertQueue(karoo, scope)
    val recorder = RideRecorder(hub, settings, RideSnapshotStore(app), scope)
    val weather = WeatherRepository(hub, settings, scope)
    val workout = WorkoutMonitor(hub, settings, alerts, scope, fieldVisible = { fieldVisible() }) { state, now ->
        coach.state.value?.nextStep(state.stepNumber - 1, state.stepCount, now)?.inWatts(hub.profile.value.ftp)
    }
    val coach = CoachLink(app, settings, scope) { effort() }
    val routes = RouteTracker(hub)
    val segments = SegmentMonitor(hub, settings, scope)
    val panels = PanelController(hub, workout, coach, settings, scope, climb = { routes.climb() }, segment = { segments.state.value })
    val mapLayers = io.github.mschache.karoopro.map.MapLayers(
        hub, routes, settings, scope, io.github.mschache.karoopro.map.MapLayerMemory(java.io.File(app.filesDir, "map-layers.txt")),
    )

    /** What Karoo Coach's check-ins carry from KarooPRO. */
    private fun effort(): Map<String, Double> {
        val s = settings.value
        return CoachEffort.values(
            wPrimeJ = hub.value(Metric.W_PRIME),
            wPrimeMaxJ = s.wPrimeJoules.toDouble(),
            cpW = (s.criticalPower ?: hub.profile.value.ftp).toDouble(),
            best = recorder.read { CoachContract.BEST.keys.associateWith { best.bestFor(it) } },
            wind = weather.wind(),
            step = workout.state.value,
            lastStep = workout.lastFinished.value,
        )
    }

    /** True when the visible ride page has a KarooPRO field on it. */
    fun fieldVisible(): Boolean =
        hub.activePage.value?.elements?.any { it.dataTypeId.startsWith(DataType.dataTypeId(EXTENSION_ID, "")) } == true

    fun cycleBigMetric() {
        scope.launch {
            val hasPower = panels.demoMode ||
                hub.isStreaming(DataType.Type.SMOOTHED_3S_AVERAGE_POWER) || hub.isStreaming(DataType.Type.POWER)
            settingsRepository.update { s ->
                // Without a power meter the power-based numbers show speed anyway, so skip them.
                val cycle = s.bigCycle(hasPower)
                val next = cycle[(cycle.indexOf(s.shownBigMetric(hasPower)) + 1) % cycle.size]
                if (next in s.bigMetrics) s.copy(bigMetric = next) else s
            }
            panels.invalidate()
        }
    }

    fun start() {
        karoo.connect { connected -> Log.i("KarooPRO", "Karoo system connected=$connected") }
        recorder.start()
        weather.start()
        workout.start()
        coach.start()
        segments.start()
        panels.start()
    }
}

class KarooProApp : Application() {
    val graph: AppGraph by lazy { AppGraph(this).also { it.start() } }
}

val Context.graph: AppGraph get() = (applicationContext as KarooProApp).graph
