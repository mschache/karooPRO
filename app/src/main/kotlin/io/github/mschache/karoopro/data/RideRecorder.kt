package io.github.mschache.karoopro.data

import android.content.Context
import android.util.Log
import io.github.mschache.karoopro.calc.BestEfforts
import io.github.mschache.karoopro.calc.FuelDue
import io.github.mschache.karoopro.calc.FuelItem
import io.github.mschache.karoopro.calc.FuelSnapshot
import io.github.mschache.karoopro.calc.FuelTracker
import io.github.mschache.karoopro.calc.ProfilePoint
import io.github.mschache.karoopro.calc.SampleHistory
import io.github.mschache.karoopro.calc.StaminaModel
import io.github.mschache.karoopro.calc.TerrainHistory
import io.github.mschache.karoopro.calc.TimeInZone
import io.github.mschache.karoopro.calc.WPrimeBalance
import io.github.mschache.karoopro.calc.ZoneMath
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.RideState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Computes what the Karoo does not provide (best efforts, W' balance, stamina, carbs burned and
 * eaten, time in zones, 1 Hz history for mini graphs and effort bars, terrain trace) once per
 * second while a ride is recording.
 */
class RideRecorder(
    private val hub: RideDataHub,
    private val settings: StateFlow<Settings>,
    private val store: RideSnapshotStore,
    private val scope: CoroutineScope,
) {
    val best = BestEfforts()
    val wPrime = WPrimeBalance(cp = 200.0, wPrime = 20_000.0)
    val powerZoneTime = TimeInZone(7)
    val hrZoneTime = TimeInZone(5)
    val history: Map<Metric, SampleHistory> = Metric.graphed.associateWith { SampleHistory(HISTORY_SECONDS) }
    val terrain = TerrainHistory()
    val stamina = StaminaModel()
    val fuel = FuelTracker()
    private var lastCalories: Double? = null

    private val _fuelDue = MutableSharedFlow<FuelDue>(extraBufferCapacity = 2)
    /** Time to eat or drink, by the targets in the settings. */
    val fuelDue: SharedFlow<FuelDue> = _fuelDue

    @Volatile var hasPower = false
        private set

    private val lock = Any()
    private var keepAlive: Job? = null

    /** Streams the recorder reads every second while a ride is active. */
    private val streams: Set<String> = buildSet {
        add(DataType.Type.POWER)
        add(DataType.Type.SMOOTHED_3S_AVERAGE_POWER)
        add(DataType.Type.HEART_RATE)
        add(DataType.Type.DISTANCE)
        add(DataType.Type.PRESSURE_ELEVATION_CORRECTION)
        add(DataType.Type.CALORIES)
        add(DataType.Type.PERCENT_MAX_HR)
        Metric.graphed.mapNotNullTo(this) { it.karoo?.typeId }
    }

    fun <T> read(block: RideRecorder.() -> T): T = synchronized(lock) { block() }

    fun start() {
        hub.registerComputed(Metric.W_PRIME) { if (hasPower) synchronized(lock) { wPrime.balance } else null }
        hub.registerComputed(Metric.W_PRIME_EMPTY) {
            val power = hub.value(Metric.POWER_3S)
            if (hasPower) synchronized(lock) { wPrime.secondsToEmpty(power) }?.times(1000) else null
        }
        hub.registerComputed(Metric.STAMINA) { if (hasPower) synchronized(lock) { stamina.potential * 100 } else null }
        hub.registerComputed(Metric.STAMINA_NOW) { if (hasPower) synchronized(lock) { stamina.now(wPrime.fraction) * 100 } else null }
        hub.registerComputed(Metric.EFFORT_LEFT) {
            val power = hub.value(Metric.POWER_3S)
            val ftp = hub.profile.value.ftp.toDouble()
            val cp = (settings.value.criticalPower ?: hub.profile.value.ftp).toDouble()
            if (hasPower) synchronized(lock) { stamina.secondsLeft(power, ftp, cp, wPrime.secondsToEmpty(power)) }?.times(1000) else null
        }
        hub.registerComputed(Metric.CARBS_BURNED) { synchronized(lock) { fuel.state.burnedG } }
        hub.registerComputed(Metric.CARBS_EATEN) { synchronized(lock) { fuel.state.eatenG } }
        hub.registerComputed(Metric.CARBS_BALANCE) { synchronized(lock) { fuel.state.eatenG - fuel.state.burnedG } }
        scope.launch {
            var previous: RideState? = null
            hub.rideState.collect { state ->
                if (state == null) return@collect
                val active = state is RideState.Recording || state is RideState.Paused
                when {
                    // First state this process sees is a running ride: the process restarted mid-ride.
                    previous == null && active -> restore()
                    previous is RideState.Idle && active -> reset()
                    previous != null && previous !is RideState.Idle && state is RideState.Idle -> store.clear()
                }
                if (active && keepAlive == null) keepAlive = hub.keepAlive(streams)
                if (!active) {
                    keepAlive?.cancel()
                    keepAlive = null
                }
                previous = state
            }
        }
        scope.launch {
            var ticks = 0
            while (isActive) {
                delay(1000)
                if (!hub.isRecording) continue
                tick()
                if (++ticks % 60 == 0) save()
            }
        }
    }

    private fun tick() {
        val profile = hub.profile.value
        val s = settings.value
        val power = hub.value(Metric.POWER)
        val power3s = hub.value(Metric.POWER_3S)
        val hr = hub.value(Metric.HEART_RATE)
        val powerNow = hub.isStreaming(DataType.Type.POWER)
        synchronized(lock) {
            if (powerNow) hasPower = true
            if (powerNow) best.add(power)
            wPrime.cp = (s.criticalPower ?: profile.ftp).toDouble()
            wPrime.wPrime = s.wPrimeJoules.toDouble()
            if (powerNow) wPrime.update(power)
            if (powerNow) powerZoneTime.add(ZoneMath.zoneOf(power3s ?: power, profile.powerZones))
            if (hr != null) hrZoneTime.add(ZoneMath.zoneOf(hr, profile.hrZones))
            val cp = (s.criticalPower ?: profile.ftp).toDouble()
            if (powerNow) stamina.update(power, profile.ftp.toDouble(), cp)
            // Carbs: from power when there is a power meter, else from the Karoo's calorie estimate.
            val calories = hub.value(Metric.CALORIES)
            val kcal = if (calories != null && lastCalories != null) (calories - lastCalories!!).coerceAtLeast(0.0) else null
            lastCalories = calories ?: lastCalories
            val hrIntensity = hub.value(Metric.HR_PERCENT_MAX)?.let { FuelTracker.hrIntensity(it) }
            fuel.addSecond(if (powerNow) power ?: 0.0 else null, profile.ftp.toDouble(), kcal, hrIntensity)
            if (s.fuelReminders) fuel.due(s.carbsPerHour, s.fuelSizes, s.drinkMlPerHour)?.let { _fuelDue.tryEmit(it) }
            history.forEach { (metric, h) -> h.add(hub.value(metric)) }
            terrain.add(hub.value(Metric.DISTANCE), hub.value(Metric.ELEVATION))
        }
    }

    /** The rider ate or drank [item]; saved right away so a restart doesn't lose it. */
    fun logFuel(item: FuelItem) {
        synchronized(lock) { fuel.log(item, settings.value.fuelSizes) }
        scope.launch { save() }
    }

    private fun reset() = synchronized(lock) {
        Log.i(TAG, "New ride: resetting recorder")
        best.reset()
        wPrime.reset()
        powerZoneTime.reset(hub.profile.value.powerZones.size.coerceAtLeast(1))
        hrZoneTime.reset(hub.profile.value.hrZones.size.coerceAtLeast(1))
        history.values.forEach { it.clear() }
        terrain.reset()
        stamina.reset()
        fuel.reset()
        lastCalories = null
        hasPower = false
        scope.launch { store.clear() }
    }

    private suspend fun save() {
        val snapshot = synchronized(lock) {
            RecorderSnapshot(
                savedAt = System.currentTimeMillis(),
                hasPower = hasPower,
                best = best.best.toList(),
                wPrime = wPrime.balance,
                powerZones = powerZoneTime.seconds.toList(),
                hrZones = hrZoneTime.seconds.toList(),
                history = history.mapKeys { it.key.name }.mapValues { (_, h) -> h.toArray().toList() },
                terrain = terrain.all().map { listOf(it.distance, it.elevation) },
                staminaUsed = stamina.used,
                fuel = fuel.state,
            )
        }
        store.save(snapshot)
    }

    private fun restore() {
        scope.launch {
            val snap = store.load() ?: return@launch
            if (System.currentTimeMillis() - snap.savedAt > 12 * 3600_000L) return@launch
            synchronized(lock) {
                hasPower = snap.hasPower
                best.restore(snap.best.toDoubleArray())
                wPrime.restore(snap.wPrime)
                powerZoneTime.restore(snap.powerZones.toLongArray())
                hrZoneTime.restore(snap.hrZones.toLongArray())
                snap.history.forEach { (name, values) ->
                    runCatching { Metric.valueOf(name) }.getOrNull()?.let { history[it]?.restore(values.toDoubleArray()) }
                }
                terrain.restore(snap.terrain.filter { it.size == 2 }.map { ProfilePoint(it[0], it[1]) })
                stamina.restore(snap.staminaUsed)
                fuel.restore(snap.fuel)
            }
            Log.i(TAG, "Restored ride snapshot from ${snap.savedAt}")
        }
    }

    companion object {
        const val TAG = "KarooPRO"
        /** 10 minutes of 1 Hz history: effort bars and mini graphs. */
        const val HISTORY_SECONDS = 600
    }
}

@Serializable
data class RecorderSnapshot(
    val savedAt: Long,
    val hasPower: Boolean,
    val best: List<Double>,
    val wPrime: Double,
    val powerZones: List<Long>,
    val hrZones: List<Long>,
    val history: Map<String, List<Double>>,
    val terrain: List<List<Double>>,
    val staminaUsed: Double = 0.0,
    val fuel: FuelSnapshot = FuelSnapshot(),
)

/** Keeps the recorder state on disk so a process restart mid-ride does not lose it. */
class RideSnapshotStore(context: Context) {
    private val file = File(context.filesDir, "ride-snapshot.json")
    private val json = Json {
        ignoreUnknownKeys = true
        allowSpecialFloatingPointValues = true
    }

    suspend fun save(snapshot: RecorderSnapshot) = withContext(Dispatchers.IO) {
        runCatching {
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(json.encodeToString(RecorderSnapshot.serializer(), snapshot))
            tmp.renameTo(file)
        }.onFailure { Log.w(RideRecorder.TAG, "Snapshot save failed", it) }
    }

    suspend fun load(): RecorderSnapshot? = withContext(Dispatchers.IO) {
        runCatching { json.decodeFromString(RecorderSnapshot.serializer(), file.readText()) }.getOrNull()
    }

    suspend fun clear() = withContext(Dispatchers.IO) { file.delete() }
}
