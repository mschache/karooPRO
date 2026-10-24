package io.github.mschache.karoopro.calc

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * The Karoo's Strava Live Segment values at one moment: times in ms, distances and elevation in
 * meters. Deltas are negative when ahead (faster than the PR, KOM or carrot), as on the Karoo's own
 * segment fields.
 */
data class SegmentReading(
    val elapsed: Double? = null,
    val prDelta: Double? = null,
    val komDelta: Double? = null,
    val carrotDelta: Double? = null,
    val pr: Double? = null,
    val kom: Double? = null,
    val distanceLeft: Double? = null,
    val elevationLeft: Double? = null,
    val totalDistance: Double? = null,
    val distanceAway: Double? = null,
) {
    /** A segment is running: the Karoo counts its time or the distance left on it. */
    val running: Boolean get() = elapsed != null || distanceLeft != null

    /** 0..1 through the segment, when its length is known. */
    val progress: Float?
        get() {
            val total = totalDistance?.takeIf { it > 0 } ?: return null
            val left = distanceLeft ?: return null
            return ((total - left) / total).toFloat().coerceIn(0f, 1f)
        }
}

enum class SegmentPhase { APPROACHING, ON, DONE }

data class SegmentState(val phase: SegmentPhase, val reading: SegmentReading)

/**
 * Turns readings into what the Segment panel shows: a segment coming up within [approachM], the
 * running segment, and its result for [holdMs] after it ends.
 */
class SegmentTracker(private val holdMs: Long = 10_000, private val approachM: Double = 500.0) {
    private var last: SegmentReading? = null
    private var endedAt: Long? = null

    fun update(reading: SegmentReading?, nowMs: Long): SegmentState? {
        if (reading != null && reading.running) {
            last = reading
            endedAt = null
            return SegmentState(SegmentPhase.ON, reading)
        }
        last?.let { finished ->
            val ended = endedAt ?: nowMs.also { endedAt = it }
            if (nowMs - ended < holdMs) return SegmentState(SegmentPhase.DONE, finished)
            last = null
            endedAt = null
        }
        val away = reading?.distanceAway ?: return null
        return if (away in 0.0..approachM) SegmentState(SegmentPhase.APPROACHING, reading) else null
    }

    companion object {
        /** "-0:07" (ahead) or "+1:12" (behind) from milliseconds. */
        fun delta(ms: Double?): String {
            val s = ms?.takeIf { it.isFinite() }?.let { (it / 1000).roundToLong() } ?: return Fmt.NONE
            return (if (s < 0) "-" else "+") + Fmt.seconds(abs(s))
        }
    }
}
