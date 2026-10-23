package io.github.mschache.karoopro

import android.app.Application
import android.content.Context
import android.util.Log
import io.github.mschache.karoopro.alerts.AlertQueue
import io.github.mschache.karoopro.data.BigMetric
import io.github.mschache.karoopro.data.PanelController
import io.github.mschache.karoopro.data.RideDataHub
import io.github.mschache.karoopro.data.RideRecorder
import io.github.mschache.karoopro.data.RideSnapshotStore
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
    val workout = WorkoutMonitor(hub, settings, alerts, scope) { fieldVisible() }
    val panels = PanelController(hub, workout, settings, scope)
    val mapLayers = io.github.mschache.karoopro.map.MapLayers(hub, settings, scope)

    /** True when the visible ride page has a KarooPRO field on it. */
    fun fieldVisible(): Boolean =
        hub.activePage.value?.elements?.any { it.dataTypeId.startsWith(DataType.dataTypeId(EXTENSION_ID, "")) } == true

    fun cycleBigMetric() {
        scope.launch {
            val hasPower = panels.demoMode ||
                hub.isStreaming(DataType.Type.SMOOTHED_3S_AVERAGE_POWER) || hub.isStreaming(DataType.Type.POWER)
            settingsRepository.update { s ->
                // Without a power meter POWER shows speed anyway, so skip it.
                val shown = if (s.bigMetric == BigMetric.POWER && !hasPower) BigMetric.SPEED else s.bigMetric
                var next = BigMetric.entries[(shown.ordinal + 1) % BigMetric.entries.size]
                if (next == BigMetric.POWER && !hasPower) next = BigMetric.entries[(next.ordinal + 1) % BigMetric.entries.size]
                s.copy(bigMetric = next)
            }
            panels.invalidate()
        }
    }

    fun start() {
        karoo.connect { connected -> Log.i("KarooPRO", "Karoo system connected=$connected") }
        recorder.start()
        weather.start()
        workout.start()
        panels.start()
    }
}

class KarooProApp : Application() {
    val graph: AppGraph by lazy { AppGraph(this).also { it.start() } }
}

val Context.graph: AppGraph get() = (applicationContext as KarooProApp).graph
