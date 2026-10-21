package io.github.mschache.karoopro.calc

/** One zone from the rider's Karoo profile, both bounds inclusive. */
data class ZoneRange(val min: Int, val max: Int)

enum class ZoneKind { POWER, HR }

object ZoneMath {
    /**
     * 1-based zone of [value] in [zones]. Values below zone 1 count as zone 1 and values above the
     * last zone count as the last zone, matching how the Karoo shows its own zone fields.
     * Gaps between zones (e.g. 0..55 then 56..75 with value 55.6) belong to the lower zone.
     */
    fun zoneOf(value: Double?, zones: List<ZoneRange>): Int? {
        if (value == null || zones.isEmpty() || value.isNaN()) return null
        for ((i, z) in zones.withIndex()) {
            if (value < z.min) return maxOf(1, i)
            if (value <= z.max) return i + 1
            val next = zones.getOrNull(i + 1)
            if (next != null && value < next.min) return i + 1
        }
        return zones.size
    }

    /** How far through its zone [value] is, 0..1, for the zone ladder. */
    fun fractionInZone(value: Double?, zones: List<ZoneRange>): Float? {
        val zone = zoneOf(value, zones) ?: return null
        val z = zones[zone - 1]
        val span = (z.max - z.min).takeIf { it > 0 } ?: return 1f
        return ((value!! - z.min) / span).toFloat().coerceIn(0f, 1f)
    }
}

/**
 * Karoo's own zone colors, as sampled by the open-source BigNum extension (Apache-2.0).
 * Heart rate has 5 zones and power 7; both share the first four colors.
 */
object ZoneColors {
    private const val RECOVERY = 0xFF60EEB2.toInt()
    private const val ENDURANCE = 0xFF00B988.toInt()
    private const val TEMPO = 0xFFFFF500.toInt()
    private const val THRESHOLD = 0xFFFB8C65.toInt()
    private const val VO2 = 0xFFFE581F.toInt()
    private const val ANAEROBIC = 0xFFD60404.toInt()
    private const val NEUROMUSCULAR = 0xFFB700A2.toInt()

    private val hr = intArrayOf(RECOVERY, ENDURANCE, TEMPO, THRESHOLD, ANAEROBIC)
    private val power = intArrayOf(RECOVERY, ENDURANCE, TEMPO, THRESHOLD, VO2, ANAEROBIC, NEUROMUSCULAR)

    /** Color for 1-based [zone] out of [zoneCount]; custom zone counts are spread over the palette. */
    fun color(kind: ZoneKind, zone: Int, zoneCount: Int): Int {
        val palette = if (kind == ZoneKind.HR) hr else power
        if (zoneCount <= 0) return palette.first()
        val z = zone.coerceIn(1, zoneCount)
        if (zoneCount == palette.size) return palette[z - 1]
        if (zoneCount == 1) return palette.first()
        val idx = Math.round((z - 1).toFloat() / (zoneCount - 1) * (palette.size - 1))
        return palette[idx.coerceIn(0, palette.size - 1)]
    }

    /** Black or white, whichever reads better on [background] (WCAG relative luminance). */
    fun onColor(background: Int): Int =
        if (relativeLuminance(background) > 0.179) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()

    fun relativeLuminance(color: Int): Double {
        fun channel(shift: Int): Double {
            val c = ((color shr shift) and 0xFF) / 255.0
            return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    /** Mix [top] over [bottom] with [alpha] (0..1). */
    fun blend(top: Int, bottom: Int, alpha: Float): Int {
        fun ch(c: Int, s: Int) = (c shr s) and 0xFF
        fun mix(s: Int) = (ch(top, s) * alpha + ch(bottom, s) * (1 - alpha)).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (mix(16) shl 16) or (mix(8) shl 8) or mix(0)
    }
}
