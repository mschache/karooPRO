package io.github.mschache.karoopro.calc

import kotlin.math.max

/** Fixed-size ring buffer of 1 Hz samples; null marks a missing sample. */
class SampleHistory(val capacity: Int) {
    private val values = DoubleArray(capacity) { Double.NaN }
    private var next = 0
    var size = 0
        private set

    fun add(value: Double?) {
        values[next] = value ?: Double.NaN
        next = (next + 1) % capacity
        if (size < capacity) size++
    }

    /** Oldest first; NaN for missing samples. */
    fun toArray(): DoubleArray {
        val out = DoubleArray(size)
        val start = (next - size + capacity) % capacity
        for (i in 0 until size) out[i] = values[(start + i) % capacity]
        return out
    }

    /** The newest [n] samples, oldest first. */
    fun last(n: Int): DoubleArray {
        val all = toArray()
        return if (all.size <= n) all else all.copyOfRange(all.size - n, all.size)
    }

    fun clear() {
        values.fill(Double.NaN)
        next = 0
        size = 0
    }

    fun restore(samples: DoubleArray) {
        clear()
        for (i in maxOf(0, samples.size - capacity) until samples.size) {
            add(samples[i].takeUnless { it.isNaN() })
        }
    }
}

/**
 * Best average power over fixed durations, updated once per second in O(durations).
 * Running sum per window over a shared ring buffer, as in karoo-critical-power's PowerBuffer.
 */
class BestEfforts(val durations: IntArray = intArrayOf(5, 60, 300, 1200)) {
    private val window = durations.max()
    private val buffer = DoubleArray(window)
    private var count = 0L
    private val sums = DoubleArray(durations.size)
    val best = DoubleArray(durations.size)

    fun add(power: Double?) {
        val p = power?.takeIf { it.isFinite() && it >= 0 } ?: 0.0
        val slot = (count % window).toInt()
        for (i in durations.indices) {
            val d = durations[i]
            sums[i] += p
            if (count >= d) sums[i] -= buffer[((count - d) % window).toInt()]
        }
        buffer[slot] = p
        count++
        for (i in durations.indices) {
            if (count >= durations[i]) best[i] = max(best[i], sums[i] / durations[i])
        }
    }

    fun reset() {
        buffer.fill(0.0)
        sums.fill(0.0)
        best.fill(0.0)
        count = 0
    }

    fun bestFor(seconds: Int): Double? = durations.indexOf(seconds).takeIf { it >= 0 }
        ?.let { best[it].takeIf { v -> v > 0 } }

    fun restore(values: DoubleArray) {
        reset()
        values.copyInto(best, endIndex = minOf(values.size, best.size))
    }
}

/**
 * W' balance with Skiba's differential model (Skiba et al. 2015, as used by most head units):
 * above CP the balance drains by (P - CP)·dt; below CP it recovers by (CP - P)·dt·(W'bal_gap / W').
 */
class WPrimeBalance(var cp: Double, var wPrime: Double) {
    var balance: Double = wPrime
        private set

    fun update(power: Double?, dtSeconds: Double = 1.0) {
        if (cp <= 0 || wPrime <= 0) return
        val p = power?.takeIf { it.isFinite() && it >= 0 } ?: 0.0
        balance = if (p > cp) {
            balance - (p - cp) * dtSeconds
        } else {
            balance + (cp - p) * dtSeconds * (wPrime - balance) / wPrime
        }
        if (balance > wPrime) balance = wPrime
    }

    val fraction: Double get() = if (wPrime > 0) balance / wPrime else 0.0

    fun reset() {
        balance = wPrime
    }

    fun restore(value: Double) {
        balance = value.coerceAtMost(wPrime)
    }
}

/** Seconds spent in each 1-based zone. */
class TimeInZone(zoneCount: Int = 7) {
    var seconds = LongArray(zoneCount)
        private set

    fun add(zone: Int?, dtSeconds: Int = 1) {
        if (zone == null || zone < 1) return
        if (zone > seconds.size) seconds = seconds.copyOf(zone)
        seconds[zone - 1] += dtSeconds.toLong()
    }

    fun reset(zoneCount: Int = seconds.size) {
        seconds = LongArray(zoneCount)
    }

    fun restore(values: LongArray) {
        seconds = values.copyOf()
    }
}

/** Averages of the newest [windowSeconds] samples split into [bars] equal buckets, oldest first. */
fun bucketAverages(samples: DoubleArray, windowSeconds: Int, bars: Int): List<Double?> {
    val per = windowSeconds / bars
    val padded = DoubleArray(windowSeconds) { Double.NaN }
    val take = minOf(samples.size, windowSeconds)
    samples.copyInto(padded, destinationOffset = windowSeconds - take, startIndex = samples.size - take)
    return (0 until bars).map { b ->
        var sum = 0.0
        var n = 0
        for (i in b * per until (b + 1) * per) {
            val v = padded[i]
            if (!v.isNaN()) {
                sum += v
                n++
            }
        }
        if (n == 0) null else sum / n
    }
}

enum class Trend { UP, DOWN, SAME, NONE }

object LapCompare {
    /** Direction of [current] against [previous]; changes under [relativeThreshold] count as SAME. */
    fun trend(current: Double?, previous: Double?, relativeThreshold: Double = 0.01): Trend {
        if (current == null || previous == null || previous == 0.0) return Trend.NONE
        val delta = (current - previous) / kotlin.math.abs(previous)
        return when {
            delta > relativeThreshold -> Trend.UP
            delta < -relativeThreshold -> Trend.DOWN
            else -> Trend.SAME
        }
    }
}
