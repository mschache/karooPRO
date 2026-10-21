package io.github.mschache.karoopro.calc

import kotlin.math.cos
import kotlin.math.sin

enum class WindKind { HEAD, CROSS, TAIL }

data class WindComponents(
    /** Positive = headwind, negative = tailwind (same unit as the wind speed). */
    val head: Double,
    /** Positive = from the right, negative = from the left. */
    val cross: Double,
    /** Where the wind comes from relative to the direction of travel, -180..180 (0 = straight ahead). */
    val relativeFromDeg: Double,
) {
    val kind: WindKind
        get() = when {
            kotlin.math.abs(relativeFromDeg) <= 60 -> WindKind.HEAD
            kotlin.math.abs(relativeFromDeg) >= 120 -> WindKind.TAIL
            else -> WindKind.CROSS
        }
}

object Wind {
    /** Normalize any angle to (-180, 180]. */
    fun normalize(deg: Double): Double {
        var d = deg % 360.0
        if (d > 180) d -= 360
        if (d <= -180) d += 360
        return d
    }

    /**
     * Split a wind into head and cross components for a rider heading [headingDeg].
     * [windFromDeg] is the meteorological direction (where the wind blows from, 0 = north).
     */
    fun components(speed: Double, windFromDeg: Double, headingDeg: Double): WindComponents {
        val rel = normalize(windFromDeg - headingDeg)
        val rad = Math.toRadians(rel)
        return WindComponents(head = speed * cos(rad), cross = speed * sin(rad), relativeFromDeg = rel)
    }
}
