package io.github.mschache.karoopro.calc

import kotlin.math.pow

/**
 * A free real-time stamina estimate, built from what KarooPro already knows:
 * - **Long term ([potential]):** how much of the day's endurance is left. Each second at a power
 *   uses 1 / [limitSeconds] of it (Miner's rule), where [limitSeconds] is how long that power could
 *   be held fresh: P(t) = FTP · (t / 1 h)^-0.07, the usual long-duration power-duration slope.
 * - **Short term ([now]):** the W' balance fraction, never above the long-term potential.
 *
 * Above CP the long-term curve is far too steep (it would give 106 s at 128 % FTP), so there W'
 * decides how long an effort lasts, and the long-term budget only counts power up to CP.
 *
 * It is an estimate from FTP, CP and W', not a measurement; the UI says so.
 */
class StaminaModel {
    /** Fraction of the long-term capacity used this ride, 0..1. */
    var used = 0.0
        private set

    fun update(power: Double?, ftp: Double, cp: Double = ftp, dtSeconds: Double = 1.0) {
        val p = power?.takeIf { it.isFinite() && it > 0 } ?: return
        if (ftp <= 0 || p < ftp * EASY) return
        used = (used + dtSeconds / limitSeconds(minOf(p, cp), ftp)).coerceAtMost(1.0)
    }

    /** Long-term stamina left, 0..1. */
    val potential: Double get() = 1 - used

    /** Short-term stamina: the W' balance fraction, capped by the long-term potential. */
    fun now(wPrimeFraction: Double?): Double = minOf(potential, wPrimeFraction ?: potential)

    /**
     * Seconds the rider could keep [power] up from here: W' running out above CP, else the
     * long-term capacity left at that power. Null when easy enough to go on all day.
     */
    fun secondsLeft(power: Double?, ftp: Double, cp: Double, wPrimeSecondsLeft: Double?): Double? {
        val p = power?.takeIf { it.isFinite() && it > 0 } ?: return null
        if (ftp <= 0) return null
        if (p > cp) return wPrimeSecondsLeft
        if (p < ftp * EASY) return null
        return (potential * limitSeconds(p, ftp)).takeIf { it < DAY_S }
    }

    fun reset() {
        used = 0.0
    }

    fun restore(value: Double) {
        used = value.coerceIn(0.0, 1.0)
    }

    companion object {
        /** Below this share of FTP riding doesn't drain the long-term budget (recovery pace). */
        const val EASY = 0.55
        private const val EXPONENT = 0.07
        private const val DAY_S = 24 * 3600.0

        /** How long [power] could be held fresh, in seconds (capped at a day). */
        fun limitSeconds(power: Double, ftp: Double): Double =
            (3600.0 * (power / ftp).pow(-1 / EXPONENT)).coerceIn(1.0, DAY_S)
    }
}
