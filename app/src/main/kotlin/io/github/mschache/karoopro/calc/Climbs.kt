package io.github.mschache.karoopro.calc

import kotlin.math.max
import kotlin.math.min

/** A route elevation profile looked up by distance, interpolated between points. */
class ElevationProfile(points: List<ProfilePoint>) {
    private val sorted = points.sortedBy { it.distance }

    val isEmpty: Boolean get() = sorted.size < 2
    val start: Double get() = sorted.firstOrNull()?.distance ?: 0.0
    val end: Double get() = sorted.lastOrNull()?.distance ?: 0.0

    /** Elevation at [d] meters along the route, clamped to the profile's ends. */
    fun at(d: Double): Double {
        if (sorted.isEmpty()) return 0.0
        if (d <= sorted.first().distance) return sorted.first().elevation
        if (d >= sorted.last().distance) return sorted.last().elevation
        var lo = 0
        var hi = sorted.size - 1
        while (hi - lo > 1) {
            val mid = (lo + hi) / 2
            if (sorted[mid].distance <= d) lo = mid else hi = mid
        }
        val a = sorted[lo]
        val b = sorted[hi]
        val t = if (b.distance > a.distance) (d - a.distance) / (b.distance - a.distance) else 0.0
        return a.elevation + (b.elevation - a.elevation) * t
    }

    /** Grade in percent around [d], measured over [window] meters. */
    fun grade(d: Double, window: Double = 200.0): Double = (at(d + window / 2) - at(d - window / 2)) / window * 100
}

/** A climb from [start] to [end] (meters along the route), the top at [end]. */
data class Climb(val start: Double, val end: Double, val gain: Double, val avgGrade: Double, val maxGrade: Double) {
    val length: Double get() = end - start
}

/** Where the rider is relative to the climb they are on, or the next one. */
data class ClimbProgress(
    /** 1-based number of [climb] among the route's [total] climbs. */
    val number: Int,
    val total: Int,
    val climb: Climb,
    /** Meters to the climb's start; zero or less once on it. */
    val toStart: Double,
    /** Meters and elevation from the rider (or the start, before it) to the top. */
    val toTop: Double,
    val elevationToTop: Double,
    /** Average grade from here to the top, in percent. */
    val avgGradeLeft: Double?,
) {
    val onClimb: Boolean get() = toStart <= 0
}

/** A stretch of a climb's profile, [from]/[to] in meters from the climb's start. */
data class GradeSlice(val from: Double, val to: Double, val fromElevation: Double, val toElevation: Double) {
    val grade: Double get() = if (to > from) (toElevation - fromElevation) / (to - from) * 100 else 0.0
}

/** The climb to show, with its profile in slices and the rider's place on it (meters from its start, null before it). */
data class ClimbView(val progress: ClimbProgress, val slices: List<GradeSlice>, val riderAt: Double?)

object Climbs {
    /**
     * Climbs along [profile]: stretches whose grade, measured over [window] meters every [step],
     * stays at [minGrade] or more and that gain at least [minGain] meters. A dip no longer than
     * [maxDip] meters that loses less than [maxDipLoss] meters doesn't split a climb. Each climb
     * starts at its lowest and ends at its highest point.
     */
    fun detect(
        profile: ElevationProfile,
        step: Double = 100.0,
        window: Double = 200.0,
        minGrade: Double = 3.0,
        minGain: Double = 30.0,
        maxDip: Double = 200.0,
        maxDipLoss: Double = 10.0,
    ): List<Climb> {
        if (profile.isEmpty) return emptyList()
        // Runs of climbing steps, [from, to].
        val runs = ArrayList<DoubleArray>()
        var d = profile.start
        while (d < profile.end) {
            val to = min(d + step, profile.end)
            if (profile.grade((d + to) / 2, window) >= minGrade) {
                val last = runs.lastOrNull()
                if (last != null && last[1] >= d - 1e-6) last[1] = to else runs += doubleArrayOf(d, to)
            }
            d = to
        }
        // Join runs split by a short, shallow dip.
        val joined = ArrayList<DoubleArray>()
        for (r in runs) {
            val last = joined.lastOrNull()
            if (last != null && r[0] - last[1] <= maxDip && lowest(profile, last[1], r[0]) >= profile.at(last[1]) - maxDipLoss) {
                last[1] = r[1]
            } else {
                joined += r.copyOf()
            }
        }
        return joined.mapNotNull { r ->
            // The grade window blurs the ends; settle them on the real foot and top nearby.
            val top = highest(profile, max(r[0], r[1] - window), min(profile.end, r[1] + window))
            val foot = lowestAt(profile, max(profile.start, r[0] - window), min(top, r[0] + window))
            val gain = profile.at(top) - profile.at(foot)
            if (top - foot < step || gain < minGain) return@mapNotNull null
            var maxGrade = 0.0
            var x = foot
            while (x < top) {
                maxGrade = max(maxGrade, profile.grade(min(x + step / 2, top), window))
                x += step
            }
            Climb(foot, top, gain, gain / (top - foot) * 100, maxGrade)
        }
    }

    /** The climb the rider is on at [along], or the next one; null past the last climb. */
    fun progress(climbs: List<Climb>, along: Double, profile: ElevationProfile): ClimbProgress? {
        val i = climbs.indexOfFirst { it.end > along }
        if (i < 0) return null
        val c = climbs[i]
        val from = max(along, c.start)
        val toTop = c.end - from
        val up = (profile.at(c.end) - profile.at(from)).coerceAtLeast(0.0)
        return ClimbProgress(
            number = i + 1,
            total = climbs.size,
            climb = c,
            toStart = c.start - along,
            toTop = toTop,
            elevationToTop = up,
            avgGradeLeft = if (toTop >= 10) up / toTop * 100 else null,
        )
    }

    /** [progress] plus the climb's profile in about [count] slices for drawing. */
    fun view(climbs: List<Climb>, along: Double, profile: ElevationProfile, count: Int = 24): ClimbView? {
        val p = progress(climbs, along, profile) ?: return null
        val c = p.climb
        val width = c.length / count
        val slices = (0 until count).map { i ->
            val a = c.start + i * width
            val b = if (i == count - 1) c.end else a + width
            GradeSlice(a - c.start, b - c.start, profile.at(a), profile.at(b))
        }
        return ClimbView(p, slices, if (p.onClimb) along - c.start else null)
    }

    private fun lowest(profile: ElevationProfile, from: Double, to: Double): Double =
        scan(from, to).minOf { profile.at(it) }

    /** Where the climb really starts: the last of the lowest points, so a flat run-in isn't counted. */
    private fun lowestAt(profile: ElevationProfile, from: Double, to: Double): Double {
        val xs = scan(from, to)
        val low = xs.minOf { profile.at(it) }
        return xs.last { profile.at(it) <= low + FLAT_M }
    }

    /** Where the climb really ends: the first of the highest points, so a flat top isn't counted. */
    private fun highest(profile: ElevationProfile, from: Double, to: Double): Double {
        val xs = scan(from, to)
        val high = xs.maxOf { profile.at(it) }
        return xs.first { profile.at(it) >= high - FLAT_M }
    }

    /** Distances from [from] to [to] every 25 m, ends included. */
    private fun scan(from: Double, to: Double): List<Double> {
        if (to <= from) return listOf(from)
        val out = ArrayList<Double>()
        var x = from
        while (x < to) {
            out += x
            x += SCAN_M
        }
        out += to
        return out
    }

    private const val SCAN_M = 25.0
    private const val FLAT_M = 0.5
}
