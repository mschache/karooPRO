package io.github.mschache.karoopro.data

import io.github.mschache.karoopro.calc.ClimbResultTracker
import io.github.mschache.karoopro.calc.RideResult
import io.github.mschache.karoopro.calc.SegmentResultTracker
import io.hammerhead.karooext.models.RideState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Keeps this ride's finished climbs (on a navigated route) and Strava Live Segments with what the
 * rider did on them, newest first, for the Results panel.
 */
class ResultsRecorder(
    private val hub: RideDataHub,
    private val routes: RouteTracker,
    private val segments: SegmentMonitor,
    private val scope: CoroutineScope,
) {
    private val climbs = ClimbResultTracker()
    private val segmentResults = SegmentResultTracker()
    private val _results = MutableStateFlow<List<RideResult>>(emptyList())
    val results: StateFlow<List<RideResult>> = _results

    fun start() {
        scope.launch {
            var previous: RideState? = null
            hub.rideState.collect { state ->
                if (previous is RideState.Idle && state != null && state !is RideState.Idle) {
                    climbs.reset()
                    _results.value = emptyList()
                }
                previous = state
            }
        }
        scope.launch {
            while (isActive) {
                delay(1000)
                if (!hub.isRecording) continue
                val now = System.currentTimeMillis()
                val power = hub.value(Metric.POWER_3S)
                val found = listOfNotNull(
                    climbs.update(routes.climb(), power, hub.value(Metric.W_PRIME), now),
                    segmentResults.update(segments.state.value, power, now),
                )
                if (found.isNotEmpty()) _results.value = (found + _results.value).take(MAX)
            }
        }
    }

    private companion object {
        const val MAX = 20
    }
}
