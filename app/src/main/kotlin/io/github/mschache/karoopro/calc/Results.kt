package io.github.mschache.karoopro.calc

import kotlinx.serialization.Serializable

/** What the rider did on a finished climb or segment; [endedAt] is epoch ms. */
@Serializable
sealed interface RideResult {
    val endedAt: Long
}

@Serializable
data class ClimbResult(
    /** 1-based climb number on the route, and how many it has. */
    val number: Int,
    val total: Int,
    val length: Double,
    val gain: Double,
    val seconds: Int,
    val avgPower: Double?,
    /** Lowest W' balance on the climb, in J. */
    val wPrimeLow: Double?,
    override val endedAt: Long,
) : RideResult {
    /** Meters climbed per hour. */
    val vam: Double? get() = if (seconds > 0) gain / seconds * 3600 else null
}

@Serializable
data class SegmentResult(
    val seconds: Int,
    /** Gap to the PR and KOM in ms, negative when faster. */
    val prDelta: Double?,
    val komDelta: Double?,
    val length: Double?,
    val avgPower: Double?,
    override val endedAt: Long,
) : RideResult

/** Average of 1 Hz samples, and the lowest value of a second series. */
private class Effort {
    var startMs = 0L
    var sum = 0.0
    var count = 0
    var low: Double? = null

    fun add(power: Double?, wPrime: Double?) {
        if (power != null && power.isFinite()) {
            sum += power
            count++
        }
        if (wPrime != null) low = minOf(low ?: wPrime, wPrime)
    }

    val average: Double? get() = if (count > 0) sum / count else null
}

/**
 * Turns the climb progress along a navigated route into a result each time the rider tops out.
 * Call it once per second while recording.
 */
class ClimbResultTracker {
    private var number: Int? = null
    private var progress: ClimbProgress? = null
    private val effort = Effort()

    fun update(p: ClimbProgress?, power: Double?, wPrime: Double?, nowMs: Long): ClimbResult? {
        val onSame = p != null && p.onClimb && p.number == number
        if (onSame) {
            effort.add(power, wPrime)
            progress = p
            return null
        }
        // Left the climb: over the top (the next climb or none ahead), or a new climb started.
        val finished = progress?.takeIf { number != null }?.let { last ->
            val toppedOut = p == null || p.number > last.number
            if (!toppedOut) null else ClimbResult(
                number = last.number,
                total = last.total,
                length = last.climb.length,
                gain = last.climb.gain,
                seconds = ((nowMs - effort.startMs) / 1000).toInt(),
                avgPower = effort.average,
                wPrimeLow = effort.low,
                endedAt = nowMs,
            )
        }
        number = null
        progress = null
        if (p != null && p.onClimb) {
            number = p.number
            progress = p
            effort.startMs = nowMs
            effort.sum = 0.0
            effort.count = 0
            effort.low = null
            effort.add(power, wPrime)
        }
        return finished
    }

    fun reset() {
        number = null
        progress = null
    }
}

/** Turns the Strava Live Segment states into a result when a segment finishes. */
class SegmentResultTracker {
    private var running = false
    private val effort = Effort()

    fun update(state: SegmentState?, power: Double?, nowMs: Long): SegmentResult? {
        when (state?.phase) {
            SegmentPhase.ON -> {
                if (!running) {
                    running = true
                    effort.sum = 0.0
                    effort.count = 0
                }
                effort.add(power, null)
            }
            SegmentPhase.DONE -> if (running) {
                running = false
                val r = state.reading
                return SegmentResult(
                    seconds = ((r.elapsed ?: 0.0) / 1000).toInt(),
                    prDelta = r.prDelta,
                    komDelta = r.komDelta,
                    length = r.totalDistance,
                    avgPower = effort.average,
                    endedAt = nowMs,
                )
            }
            else -> running = false
        }
        return null
    }
}
