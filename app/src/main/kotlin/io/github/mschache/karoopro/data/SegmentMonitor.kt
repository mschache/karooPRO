package io.github.mschache.karoopro.data

import android.util.Log
import io.github.mschache.karoopro.calc.SegmentReading
import io.github.mschache.karoopro.calc.SegmentState
import io.github.mschache.karoopro.calc.SegmentTracker
import io.hammerhead.karooext.models.DataType.Field
import io.hammerhead.karooext.models.DataType.Type
import io.hammerhead.karooext.models.StreamState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Follows the Karoo's Strava Live Segment streams while a ride is active and the Segment panel is
 * on. The SDK doesn't document their units, signs or when they stream, so every change of their
 * stream states and field keys is logged (release builds too), and the values every 10 s while a
 * segment is near, for checking with `adb logcat -s KarooPro` after a ride.
 */
class SegmentMonitor(
    private val hub: RideDataHub,
    private val settings: StateFlow<Settings>,
    private val scope: CoroutineScope,
) {
    private val tracker = SegmentTracker()
    private val _state = MutableStateFlow<SegmentState?>(null)
    /** The segment coming up, running, or just finished; null otherwise. */
    val state: StateFlow<SegmentState?> = _state

    fun start() {
        scope.launch {
            var keep: Job? = null
            var lastShape = ""
            var lastValuesAt = 0L
            while (isActive) {
                val wanted = hub.isRideActive && settings.value.orderedPanels.any { it.panel == Panel.SEGMENT && it.enabled }
                if (wanted && keep == null) keep = hub.keepAlive(TYPES)
                if (!wanted) {
                    keep?.cancel()
                    keep = null
                    _state.value = null
                } else {
                    val now = System.currentTimeMillis()
                    val states = TYPES.associateWith { hub.stream(it).value }
                    val shape = states.entries.joinToString(" ") { (type, s) -> type.removePrefix("TYPE_SEGMENT_").removeSuffix("_ID") + "=" + describe(s) }
                    if (shape != lastShape) {
                        Log.i(TAG, "segment streams: $shape")
                        lastShape = shape
                    }
                    val reading = read(states)
                    if (reading != null && (reading.running || reading.distanceAway != null) && now - lastValuesAt >= VALUES_LOG_MS) {
                        Log.i(TAG, "segment values: $reading")
                        lastValuesAt = now
                    }
                    _state.value = tracker.update(reading, now)
                }
                delay(TICK_MS)
            }
        }
    }

    private fun describe(s: StreamState?): String = when (s) {
        is StreamState.Streaming -> s.dataPoint.values.keys.joinToString(",", "[", "]") { it.removePrefix("FIELD_SEGMENT_").removeSuffix("_ID") }
        null -> "none"
        else -> s.javaClass.simpleName
    }

    /** All segment fields from whichever stream carries them; null when none is streaming. */
    private fun read(states: Map<String, StreamState?>): SegmentReading? {
        val values = HashMap<String, Double>()
        states.values.forEach { s -> (s as? StreamState.Streaming)?.dataPoint?.values?.let { values.putAll(it) } }
        if (values.isEmpty()) return null
        fun v(field: String) = values[field]?.takeIf { it.isFinite() }
        return SegmentReading(
            // Zeros between segments would read as a segment running.
            elapsed = v(Field.SEGMENT_TIME_ELAPSED)?.takeIf { it > 0 },
            prDelta = v(Field.SEGMENT_PR_DELTA_TIME),
            komDelta = v(Field.SEGMENT_KOM_DELTA_TIME),
            carrotDelta = v(Field.SEGMENT_CARROT_DELTA_TIME),
            pr = v(Field.SEGMENT_PR_TIME)?.takeIf { it > 0 },
            kom = v(Field.SEGMENT_KOM_TIME)?.takeIf { it > 0 },
            distanceLeft = v(Field.SEGMENT_DISTANCE_REMAINING)?.takeIf { it > 0 },
            elevationLeft = v(Field.SEGMENT_ELEVATION_REMAINING),
            totalDistance = v(Field.SEGMENT_TOTAL_DISTANCE)?.takeIf { it > 0 },
            distanceAway = v(Field.SEGMENT_DISTANCE_AWAY),
        )
    }

    companion object {
        private const val TAG = "KarooPRO"
        private const val TICK_MS = 1_000L
        private const val VALUES_LOG_MS = 10_000L
        val TYPES = listOf(
            Type.SEGMENT_TIME,
            Type.SEGMENT_TIME_TO_PR,
            Type.SEGMENT_TIME_TO_KOM,
            Type.SEGMENT_TIME_TO_CARROT,
            Type.SEGMENT_PR,
            Type.SEGMENT_KOM,
            Type.SEGMENT_DISTANCE_REMAINING,
            Type.SEGMENT_ELEVATION_REMAINING,
        )
    }
}
