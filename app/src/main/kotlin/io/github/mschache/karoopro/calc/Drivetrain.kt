package io.github.mschache.karoopro.calc

/**
 * Current gearing as streamed by the Karoo shifting types. Gear numbers are 1-based; per Shimano and
 * SRAM convention front gear 1 is the small ring and rear gear 1 the largest (easiest) cog.
 */
data class Gearing(
    val frontGear: Int?,
    val frontMax: Int?,
    val rearGear: Int?,
    val rearMax: Int?,
    val frontTeeth: Int?,
    val rearTeeth: Int?,
    /** Chainring teeth from the saved device's gear info, any order. */
    val frontTeethList: List<Int>? = null,
    /** Cassette teeth from the saved device's gear info, any order. */
    val rearTeethList: List<Int>? = null,
) {
    val ratio: Double?
        get() = if (frontTeeth != null && rearTeeth != null && rearTeeth > 0) frontTeeth.toDouble() / rearTeeth else null

    val isOneBy: Boolean get() = (frontMax ?: 1) <= 1

    /** 0 = largest cog (easiest) .. 1 = smallest cog (hardest). */
    val rearPosition: Float?
        get() {
            val cassette = rearTeethList?.sortedDescending()
            if (cassette != null && rearTeeth != null && cassette.size > 1) {
                val idx = cassette.indexOf(rearTeeth)
                if (idx >= 0) return idx.toFloat() / (cassette.size - 1)
            }
            val g = rearGear ?: return null
            val max = rearMax ?: return null
            return if (max > 1) (g - 1).toFloat() / (max - 1) else 0f
        }

    /**
     * Big ring with one of the largest cogs, or small ring with one of the smallest. One-by drivetrains
     * never cross-chain here. The "edge" is 3 cogs on 11+ speed cassettes, otherwise 2.
     */
    val crossChained: Boolean
        get() {
            if (isOneBy) return false
            val front = frontGear ?: return false
            val fMax = frontMax ?: return false
            val pos = rearPosition ?: return false
            val cogs = rearTeethList?.size ?: rearMax ?: return false
            if (cogs < 2) return false
            val edge = if (cogs >= 11) 3 else 2
            val edgeFraction = (edge - 1).toFloat() / (cogs - 1)
            val bigRing = front >= fMax
            val smallRing = front <= 1
            return (bigRing && pos <= edgeFraction + 1e-4f) || (smallRing && pos >= 1f - edgeFraction - 1e-4f)
        }
}

/** The end of the cassette: [TOP] is the smallest (hardest) cog, [BOTTOM] the largest (easiest). */
enum class GearLimit { TOP, BOTTOM }

object GearLimits {
    /** The limit just shifted into: only when the rear gear changed and now sits on an end of the cassette. */
    fun reached(previous: Gearing?, now: Gearing?): GearLimit? {
        val gear = now?.rearGear ?: return null
        if (previous?.rearGear == null || previous.rearGear == gear) return null
        val pos = now.rearPosition ?: return null
        return when {
            pos >= 1f - 1e-4f -> GearLimit.TOP
            pos <= 1e-4f -> GearLimit.BOTTOM
            else -> null
        }
    }
}
