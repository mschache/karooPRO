package io.github.mschache.karoopro.calc

import kotlinx.serialization.Serializable

/** What the rider can log with a tap or a button. */
enum class FuelItem(val label: String) { GEL("GEL"), BAR("BAR"), BOTTLE("BOTTLE") }

/** Carbs per item and the drink in a bottle, from the settings. */
data class FuelSizes(val gelCarbs: Int, val barCarbs: Int, val bottleCarbs: Int, val bottleMl: Int) {
    fun carbs(item: FuelItem) = when (item) {
        FuelItem.GEL -> gelCarbs
        FuelItem.BAR -> barCarbs
        FuelItem.BOTTLE -> bottleCarbs
    }

    fun ml(item: FuelItem) = if (item == FuelItem.BOTTLE) bottleMl else 0
}

enum class FuelDue { EAT, DRINK }

@Serializable
data class FuelSnapshot(
    val burnedG: Double = 0.0,
    val eatenG: Double = 0.0,
    val drankMl: Double = 0.0,
    val movingS: Long = 0,
    val lastEatReminderS: Long = -1,
    val lastDrinkReminderS: Long = -1,
)

/**
 * Carbohydrate burned versus eaten, and when to eat or drink next. Burn comes from mechanical work:
 * at about 24 % gross efficiency 1 kJ of work costs about 1 kcal, and the carbohydrate share of that
 * rises with intensity (power / FTP). Without power it uses the Karoo's calorie estimate and heart
 * rate. Intake is what the rider logs.
 */
class FuelTracker {
    var state = FuelSnapshot()
        private set

    /** One moving second: [powerW] and [ftp], or the Karoo's [kcal] for this second with [hrIntensity]. */
    fun addSecond(powerW: Double?, ftp: Double, kcal: Double?, hrIntensity: Double?) {
        val grams = when {
            powerW != null && powerW.isFinite() && ftp > 0 -> carbGrams(powerW / 1000 * KCAL_PER_KJ_WORK, powerW / ftp)
            kcal != null && kcal.isFinite() && kcal > 0 -> carbGrams(kcal, hrIntensity ?: 0.6)
            else -> 0.0
        }
        state = state.copy(burnedG = state.burnedG + grams, movingS = state.movingS + 1)
    }

    fun log(item: FuelItem, sizes: FuelSizes) {
        state = state.copy(eatenG = state.eatenG + sizes.carbs(item), drankMl = state.drankMl + sizes.ml(item))
    }

    /** Carbs that should be in by now at [carbsPerHour], after a free first [GRACE_S]. */
    fun targetCarbs(carbsPerHour: Int): Double = carbsPerHour * ((state.movingS - GRACE_S).coerceAtLeast(0) / 3600.0)

    fun targetMl(mlPerHour: Int): Double = mlPerHour * ((state.movingS - GRACE_S).coerceAtLeast(0) / 3600.0)

    /** Seconds of moving time until the rider is one serving behind; 0 when already due. */
    fun secondsUntilEat(carbsPerHour: Int, serving: Int): Long? {
        if (carbsPerHour <= 0 || serving <= 0) return null
        val dueAtS = GRACE_S + (state.eatenG + serving) / carbsPerHour * 3600
        return (dueAtS - state.movingS).toLong().coerceAtLeast(0)
    }

    fun secondsUntilDrink(mlPerHour: Int, sip: Int): Long? {
        if (mlPerHour <= 0 || sip <= 0) return null
        val dueAtS = GRACE_S + (state.drankMl + sip) / mlPerHour * 3600
        return (dueAtS - state.movingS).toLong().coerceAtLeast(0)
    }

    /**
     * What to remind about now, at most once per [REPEAT_S] for each: eat when a serving behind the
     * target, drink when a quarter bottle behind.
     */
    fun due(carbsPerHour: Int, sizes: FuelSizes, mlPerHour: Int): FuelDue? {
        val now = state.movingS
        fun ready(last: Long) = last < 0 || now - last >= REPEAT_S
        if (secondsUntilEat(carbsPerHour, sizes.gelCarbs) == 0L && ready(state.lastEatReminderS)) {
            state = state.copy(lastEatReminderS = now)
            return FuelDue.EAT
        }
        if (secondsUntilDrink(mlPerHour, sizes.bottleMl / 4) == 0L && ready(state.lastDrinkReminderS)) {
            state = state.copy(lastDrinkReminderS = now)
            return FuelDue.DRINK
        }
        return null
    }

    fun reset() {
        state = FuelSnapshot()
    }

    fun restore(snapshot: FuelSnapshot) {
        state = snapshot
    }

    companion object {
        /** kcal of metabolic energy per kJ of mechanical work (1 / 4.184 / 0.24). */
        const val KCAL_PER_KJ_WORK = 0.996
        const val GRACE_S = 20 * 60L
        const val REPEAT_S = 10 * 60L

        /** Share of energy from carbohydrate at an intensity (power / FTP), from typical crossover data. */
        fun carbShare(intensity: Double): Double {
            val points = listOf(0.4 to 0.35, 0.55 to 0.5, 0.7 to 0.65, 0.85 to 0.8, 1.0 to 0.95)
            if (intensity <= points.first().first) return points.first().second
            if (intensity >= points.last().first) return points.last().second
            val (a, b) = points.zipWithNext().first { intensity <= it.second.first }
            return a.second + (b.second - a.second) * (intensity - a.first) / (b.first - a.first)
        }

        /** Grams of carbohydrate in [kcal] at [intensity], at 4 kcal per gram. */
        fun carbGrams(kcal: Double, intensity: Double): Double = kcal * carbShare(intensity) / 4

        /** Intensity from heart rate as a share of max, for riders without power: 60 % → 0.45, 90 % → 0.95. */
        fun hrIntensity(hrPercentMax: Double): Double = (0.45 + (hrPercentMax - 60) / 30 * 0.5).coerceIn(0.3, 1.1)
    }
}
