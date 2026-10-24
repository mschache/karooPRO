package io.github.mschache.karoopro.calc

import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Rider and bike for the speed model. */
data class Rig(
    val massKg: Double,
    val cda: Double = 0.32,
    val crr: Double = 0.005,
    val drivetrainEfficiency: Double = 0.97,
    val airDensity: Double = 1.2,
)

object BikePhysics {
    private const val G = 9.81
    const val MIN_SPEED = 1.0
    /** Descents are capped here: braking for corners keeps real speeds below free-roll speed. */
    const val MAX_SPEED = 18.0

    /**
     * Steady speed in m/s at [power] watts on a [grade] (fraction, 0.05 = 5 %) into a [headwind]
     * (m/s, negative for tailwind): solves power = force × speed by bisection.
     */
    fun speed(power: Double, grade: Double, rig: Rig, headwind: Double = 0.0): Double {
        val theta = atan(grade)
        val gravityRoll = rig.massKg * G * (rig.crr * cos(theta) + sin(theta))
        val wheel = power.coerceAtLeast(0.0) * rig.drivetrainEfficiency
        fun surplus(v: Double): Double {
            val air = v + headwind
            val aero = 0.5 * rig.airDensity * rig.cda * air * kotlin.math.abs(air)
            return wheel - (gravityRoll + aero) * v
        }
        var lo = 0.1
        var hi = 40.0
        if (surplus(lo) <= 0) return MIN_SPEED
        repeat(60) {
            val mid = (lo + hi) / 2
            if (surplus(mid) > 0) lo = mid else hi = mid
        }
        return lo.coerceIn(MIN_SPEED, MAX_SPEED)
    }
}

/** One stretch of the route with its target power and planned time. */
data class PaceSection(val from: Double, val to: Double, val grade: Double, val power: Int, val seconds: Double)

/**
 * A Power Guide-style plan for a route: a base power on the flat (a share of FTP) that rises on
 * climbs and drops on descents, with the speed and time each stretch takes at that power.
 */
class PacePlan(val sections: List<PaceSection>) {
    /** Planned seconds from the start to each section's start. */
    private val startTimes: DoubleArray = DoubleArray(sections.size).also { t ->
        for (i in 1 until sections.size) t[i] = t[i - 1] + sections[i - 1].seconds
    }

    val totalSeconds: Double get() = sections.lastOrNull()?.let { startTimes.last() + it.seconds } ?: 0.0
    val length: Double get() = sections.lastOrNull()?.to ?: 0.0

    fun indexAt(along: Double): Int? {
        if (sections.isEmpty()) return null
        if (along <= 0) return 0
        val i = sections.indexOfFirst { along < it.to }
        return if (i < 0) null else i
    }

    fun sectionAt(along: Double): PaceSection? = indexAt(along)?.let { sections[it] }

    /** Planned seconds from the start of the route to [along]. */
    fun timeAt(along: Double): Double {
        val i = indexAt(along) ?: return totalSeconds
        val s = sections[i]
        val f = if (s.to > s.from) ((along - s.from) / (s.to - s.from)).coerceIn(0.0, 1.0) else 0.0
        return startTimes[i] + s.seconds * f
    }

    /**
     * The next change of target after [along]: where it starts and the power there. Neighboring
     * sections within [step] watts count as the same target.
     */
    fun nextChange(along: Double, step: Int = 15): PaceSection? {
        val i = indexAt(along) ?: return null
        val now = sections[i].power
        return sections.drop(i + 1).firstOrNull { kotlin.math.abs(it.power - now) >= step }
    }

    companion object {
        /** How much power rises per unit of grade (6 % → +18 %), and its limits. */
        private const val GRADE_GAIN = 3.0
        private const val MIN_FACTOR = 0.6
        private const val MAX_FACTOR = 1.3

        /** Target power factor for a grade (fraction). */
        fun factor(grade: Double): Double = (1 + GRADE_GAIN * grade).coerceIn(MIN_FACTOR, MAX_FACTOR)

        /**
         * Builds the plan over [profile] in [step]-meter sections: [basePower] on the flat, scaled by
         * [factor] with the grade, capped at [maxPower] (e.g. CP so no section empties W').
         */
        fun build(profile: ElevationProfile, basePower: Double, maxPower: Double, rig: Rig, headwind: Double = 0.0, step: Double = 250.0): PacePlan {
            if (profile.isEmpty || basePower <= 0) return PacePlan(emptyList())
            val out = ArrayList<PaceSection>()
            var d = profile.start
            while (d < profile.end) {
                val to = minOf(d + step, profile.end)
                val len = to - d
                val grade = if (len > 0) (profile.at(to) - profile.at(d)) / len else 0.0
                val power = (basePower * factor(grade)).coerceAtMost(maxPower)
                val v = BikePhysics.speed(power, grade, rig, headwind)
                out += PaceSection(d, to, grade, power.roundToInt(), len / v)
                d = to
            }
            return PacePlan(out)
        }
    }
}
