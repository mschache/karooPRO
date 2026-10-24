package io.github.mschache.karoopro.calc

import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** Rider unit preferences, mirrored from the Karoo UserProfile. */
data class UnitPrefs(
    val metricDistance: Boolean = true,
    val metricElevation: Boolean = true,
    val metricTemperature: Boolean = true,
)

/** Formatting helpers; every value arrives in Karoo's SI units (m, m/s, ms, °C). */
object Fmt {
    const val NONE = "--"

    fun speedValue(ms: Double, u: UnitPrefs) = if (u.metricDistance) ms * 3.6 else ms * 2.2369363
    fun speedUnit(u: UnitPrefs) = if (u.metricDistance) "km/h" else "mph"
    fun distanceUnit(u: UnitPrefs) = if (u.metricDistance) "km" else "mi"
    fun elevationUnit(u: UnitPrefs) = if (u.metricElevation) "m" else "ft"
    fun temperatureUnit(u: UnitPrefs) = if (u.metricTemperature) "°C" else "°F"
    fun pressureUnit(u: UnitPrefs) = if (u.metricDistance) "bar" else "psi"

    fun int(v: Double?): String = v?.takeIf { it.isFinite() }?.roundToLong()?.toString() ?: NONE

    fun decimal(v: Double?, digits: Int): String =
        v?.takeIf { it.isFinite() }?.let { String.format(Locale.US, "%.${digits}f", it) } ?: NONE

    fun speed(ms: Double?, u: UnitPrefs): String = ms?.let { decimal(speedValue(it, u), 1) } ?: NONE

    fun distance(m: Double?, u: UnitPrefs): String = m?.let {
        val v = if (u.metricDistance) it / 1000.0 else it / 1609.344
        decimal(v, if (v < 100) 2 else 1)
    } ?: NONE

    fun elevation(m: Double?, u: UnitPrefs): String = m?.let { int(if (u.metricElevation) it else it * 3.28084) } ?: NONE

    fun temperature(c: Double?, u: UnitPrefs, digits: Int = 0): String = c?.let {
        val v = if (u.metricTemperature) it else it * 9 / 5 + 32
        if (digits == 0) int(v) else decimal(v, digits)
    } ?: NONE

    /** Tyre pressure, which the Karoo reports in mbar: bar with metric distances, psi otherwise. */
    fun pressure(mbar: Double?, u: UnitPrefs): String = mbar?.takeIf { it > 0 }?.let {
        if (u.metricDistance) decimal(it / 1000, 2) else int(it * 0.0145038)
    } ?: NONE

    /** h:mm:ss or m:ss from milliseconds. */
    fun duration(ms: Double?): String = ms?.takeIf { it.isFinite() && it >= 0 }?.let { seconds((it / 1000).toLong()) } ?: NONE

    /**
     * Time of day as H:mm. The Karoo's clock and arrival fields are milliseconds since local
     * midnight; epoch milliseconds are accepted too.
     */
    fun clock(ms: Double?): String {
        val v = ms?.takeIf { it.isFinite() && it >= 0 } ?: return NONE
        val secondOfDay = if (v > 2 * DAY_MS) {
            Instant.ofEpochMilli(v.toLong()).atZone(ZoneId.systemDefault()).toLocalTime().toSecondOfDay().toLong()
        } else {
            (v / 1000).toLong() % 86_400
        }
        return String.format(Locale.US, "%d:%02d", secondOfDay / 3600, (secondOfDay % 3600) / 60)
    }

    private const val DAY_MS = 86_400_000.0

    fun seconds(totalSeconds: Long): String {
        val s = abs(totalSeconds)
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        val sign = if (totalSeconds < 0) "-" else ""
        return if (h > 0) String.format(Locale.US, "%s%d:%02d:%02d", sign, h, m, sec)
        else String.format(Locale.US, "%s%d:%02d", sign, m, sec)
    }

    /** Compact duration for time-in-zone: 1h05, 12m, 45s. */
    fun shortDuration(totalSeconds: Long): String = when {
        totalSeconds >= 3600 -> String.format(Locale.US, "%dh%02d", totalSeconds / 3600, (totalSeconds % 3600) / 60)
        totalSeconds >= 60 -> "${totalSeconds / 60}m"
        else -> "${totalSeconds}s"
    }

    fun percent(v: Double?): String = v?.let { int(it) } ?: NONE

    fun balance(leftPercent: Double?): String = leftPercent?.takeIf { it in 0.0..100.0 }?.let {
        val l = it.roundToInt()
        "$l/${100 - l}"
    } ?: NONE
}
