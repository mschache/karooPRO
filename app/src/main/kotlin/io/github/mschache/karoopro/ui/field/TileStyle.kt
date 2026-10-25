package io.github.mschache.karoopro.ui.field

import io.github.mschache.karoopro.calc.ZoneColors
import io.github.mschache.karoopro.data.Metric

/** Which color a Ride tile's side bar and icon take. */
enum class TileFamily { TIME, DISTANCE, ENERGY, CADENCE, POWER, HEART, OTHER }

/** A tile's icon and color family; [named] when the icon alone says what the value is, so its label is left off. */
data class TileLook(val icon: TileIcon?, val family: TileFamily, val named: Boolean = false)

object TileStyle {
    fun look(m: Metric): TileLook = when (m) {
        Metric.ELAPSED_TIME -> TileLook(TileIcon.TIMER, TileFamily.TIME, named = true)
        Metric.TOTAL_TIME -> TileLook(TileIcon.TIMER, TileFamily.TIME)
        Metric.CLOCK -> TileLook(TileIcon.CLOCK, TileFamily.TIME, named = true)
        Metric.ETA -> TileLook(TileIcon.CLOCK, TileFamily.TIME)
        Metric.SUNSET_IN -> TileLook(TileIcon.SUNSET, TileFamily.TIME)
        Metric.DISTANCE -> TileLook(TileIcon.FLAG, TileFamily.DISTANCE, named = true)
        Metric.TO_GO -> TileLook(TileIcon.FLAG, TileFamily.DISTANCE)
        Metric.CALORIES -> TileLook(TileIcon.FIRE, TileFamily.ENERGY, named = true)
        Metric.KCAL_PER_HOUR, Metric.WORK -> TileLook(TileIcon.FIRE, TileFamily.ENERGY)
        Metric.CARBS_BURNED, Metric.CARBS_EATEN, Metric.CARBS_BALANCE -> TileLook(TileIcon.FOOD, TileFamily.ENERGY)
        Metric.CADENCE -> TileLook(TileIcon.CADENCE, TileFamily.CADENCE, named = true)
        Metric.AVG_CADENCE -> TileLook(TileIcon.CADENCE, TileFamily.CADENCE)
        Metric.POWER_3S -> TileLook(TileIcon.BOLT, TileFamily.POWER, named = true)
        Metric.POWER, Metric.POWER_10S, Metric.POWER_30S, Metric.POWER_20M, Metric.POWER_1H, Metric.AVG_POWER,
        Metric.MAX_POWER, Metric.NP, Metric.FTP_PERCENT, Metric.WKG, Metric.PACE_TARGET,
        -> TileLook(TileIcon.BOLT, TileFamily.POWER)
        Metric.W_PRIME, Metric.W_PRIME_EMPTY, Metric.STAMINA, Metric.STAMINA_NOW, Metric.EFFORT_LEFT -> TileLook(TileIcon.BATTERY, TileFamily.POWER)
        Metric.BALANCE -> TileLook(TileIcon.BALANCE, TileFamily.POWER)
        Metric.HEART_RATE -> TileLook(TileIcon.HEART, TileFamily.HEART, named = true)
        Metric.AVG_HR, Metric.MAX_HR, Metric.HR_PERCENT_MAX, Metric.HRR_PERCENT -> TileLook(TileIcon.HEART, TileFamily.HEART)
        Metric.CORE_TEMP -> TileLook(TileIcon.THERMOMETER, TileFamily.HEART)
        Metric.SPEED -> TileLook(TileIcon.SPEED, TileFamily.OTHER, named = true)
        Metric.SPEED_3S, Metric.AVG_SPEED, Metric.MAX_SPEED -> TileLook(TileIcon.SPEED, TileFamily.OTHER)
        Metric.ELEVATION -> TileLook(TileIcon.MOUNTAIN, TileFamily.OTHER, named = true)
        Metric.ASCENT, Metric.CLIMB_LEFT -> TileLook(TileIcon.MOUNTAIN, TileFamily.OTHER)
        Metric.GRADE -> TileLook(TileIcon.TREND, TileFamily.OTHER, named = true)
        Metric.VAM -> TileLook(TileIcon.TREND, TileFamily.OTHER)
        Metric.TEMPERATURE -> TileLook(TileIcon.THERMOMETER, TileFamily.OTHER, named = true)
        Metric.TYRE_FRONT, Metric.TYRE_REAR -> TileLook(TileIcon.TYRE, TileFamily.OTHER)
        Metric.HEADWIND, Metric.CROSSWIND, Metric.WIND_SPEED -> TileLook(TileIcon.WIND, TileFamily.OTHER)
        else -> TileLook(null, TileFamily.OTHER)
    }

    /**
     * The side bar and icon color. Power and heart rate follow their zone, as the cards under the big
     * number do; on the light theme every color is darkened enough to read on the light card.
     */
    fun color(f: TileFamily, p: Palette, powerColor: Int?, hrColor: Int?): Int {
        fun light(c: Int) = if (p.dark) c else ZoneColors.blend(c, BLACK, 0.72f)
        return when (f) {
            TileFamily.TIME -> if (p.dark) PURPLE else PURPLE_LIGHT
            TileFamily.DISTANCE -> p.accent
            TileFamily.ENERGY -> if (p.dark) ORANGE else ORANGE_LIGHT
            TileFamily.CADENCE -> p.good
            TileFamily.POWER -> powerColor?.let(::light) ?: p.dim
            TileFamily.HEART -> hrColor?.let(::light) ?: p.bad
            TileFamily.OTHER -> p.dim
        }
    }

    private const val BLACK = 0xFF000000.toInt()
    private const val PURPLE = 0xFFB07CFF.toInt()
    private const val PURPLE_LIGHT = 0xFF7C3AED.toInt()
    private const val ORANGE = 0xFFFF8A00.toInt()
    private const val ORANGE_LIGHT = 0xFFC25E00.toInt()
}

/** The numbers behind the tiles' small graphics. */
object TileGraphics {
    private const val HOUR_MS = 3_600_000.0
    private const val DAY_MS = 86_400_000.0

    /** One segment per hour ridden, at least [min] and at most [max]: past hours full, the running one partly. */
    fun hours(elapsedMs: Double?, min: Int = 4, max: Int = 8): List<Float> {
        val h = ((elapsedMs ?: 0.0) / HOUR_MS).takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
        val n = (h.toInt() + 1).coerceIn(min, max)
        return (0 until n).map { i -> (h - i).coerceIn(0.0, 1.0).toFloat() }
    }

    /** [fraction] spread over [n] equal segments. */
    fun spread(fraction: Float, n: Int = 5): List<Float> = (0 until n).map { i -> (fraction * n - i).coerceIn(0f, 1f) }

    /** Share of the navigated route behind the rider; null without a route. */
    fun routeFraction(distance: Double?, toGo: Double?): Float? {
        if (distance == null || toGo == null || !distance.isFinite() || !toGo.isFinite() || toGo <= 0 || distance < 0) return null
        return (distance / (distance + toGo)).toFloat().coerceIn(0f, 1f)
    }

    /**
     * Share of the time from the start to the arrival: elapsed against elapsed plus what's left to
     * [arrivalMs]. The Karoo gives the clock and the arrival as milliseconds since local midnight
     * (an arrival past midnight wraps); epoch milliseconds are compared with [nowEpochMs].
     */
    fun timeFraction(elapsedMs: Double?, clockMs: Double?, arrivalMs: Double?, nowEpochMs: Long): Float? {
        if (elapsedMs == null || arrivalMs == null || !elapsedMs.isFinite() || !arrivalMs.isFinite() || elapsedMs <= 0) return null
        val left = if (arrivalMs > 2 * DAY_MS) {
            arrivalMs - nowEpochMs
        } else {
            val clock = clockMs?.takeIf { it.isFinite() } ?: return null
            (arrivalMs - clock).let { if (it < 0) it + DAY_MS else it }
        }
        if (left < 0 || left > DAY_MS / 2) return null
        return (elapsedMs / (elapsedMs + left)).toFloat().coerceIn(0f, 1f)
    }

    /** The cadence scale around the target band: 30 rpm either side. */
    fun cadenceScale(low: Int, high: Int): Pair<Float, Float> = (low - 30f).coerceAtLeast(0f) to high + 30f
}
