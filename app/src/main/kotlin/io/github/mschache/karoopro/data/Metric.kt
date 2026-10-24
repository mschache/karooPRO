package io.github.mschache.karoopro.data

import io.github.mschache.karoopro.calc.Fmt
import io.github.mschache.karoopro.calc.UnitPrefs
import io.hammerhead.karooext.models.DataType.Field
import io.hammerhead.karooext.models.DataType.Type

/** Where a metric comes from: a Karoo data type field, or something KarooPro computes itself. */
sealed class Source {
    /** [pair] is a second field averaged with [field], for left/right pedal values. */
    data class Karoo(val typeId: String, val field: String?, val pair: String? = null) : Source()
    data object Computed : Source()
}

enum class Format { POWER, SPEED, HR, CADENCE, DISTANCE, DURATION, DELTA, CLOCK, ELEVATION, GRADE, VAM, KJ, KCAL, KCAL_H, PERCENT, DECIMAL2, INT, TEMP, BODY_TEMP, PRESSURE, BALANCE, WKG, WIND, GRAMS }

/**
 * Every value a tile, card or lap field can show. [graph] names the metric whose 1 Hz history is
 * drawn as the tile's mini graph (cumulative values graph their rate instead), null for none.
 */
enum class Metric(
    val label: String,
    val source: Source,
    val format: Format,
    graphOf: String? = null,
    val selectable: Boolean = true,
) {
    POWER_3S("POWER 3S", Source.Karoo(Type.SMOOTHED_3S_AVERAGE_POWER, Field.SMOOTHED_3S_AVERAGE_POWER), Format.POWER, "POWER_3S"),
    POWER("POWER", Source.Karoo(Type.POWER, Field.POWER), Format.POWER, "POWER_3S"),
    POWER_10S("POWER 10S", Source.Karoo(Type.SMOOTHED_10S_AVERAGE_POWER, Field.SMOOTHED_10S_AVERAGE_POWER), Format.POWER, "POWER_3S"),
    POWER_30S("POWER 30S", Source.Karoo(Type.SMOOTHED_30S_AVERAGE_POWER, Field.SMOOTHED_30S_AVERAGE_POWER), Format.POWER, "POWER_3S"),
    POWER_20M("POWER 20M", Source.Karoo(Type.SMOOTHED_20M_AVERAGE_POWER, Field.SMOOTHED_20M_AVERAGE_POWER), Format.POWER, "POWER_3S"),
    POWER_1H("POWER 1H", Source.Karoo(Type.SMOOTHED_1HR_AVERAGE_POWER, Field.SMOOTHED_1HR_AVERAGE_POWER), Format.POWER, "POWER_3S"),
    AVG_POWER("AVG POWER", Source.Karoo(Type.AVERAGE_POWER, Field.AVERAGE_POWER), Format.POWER, "POWER_3S"),
    MAX_POWER("MAX POWER", Source.Karoo(Type.MAX_POWER, Field.MAX_POWER), Format.POWER),
    NP("NP", Source.Karoo(Type.NORMALIZED_POWER, Field.NORMALIZED_POWER), Format.POWER, "POWER_3S"),
    IF("IF", Source.Karoo(Type.INTENSITY_FACTOR, Field.INTENSITY_FACTOR), Format.DECIMAL2),
    TSS("TSS", Source.Karoo(Type.TRAINING_STRESS_SCORE, Field.TRAINING_STRESS_SCORE), Format.INT),
    VI("VI", Source.Karoo(Type.VARIABILITY_INDEX, Field.VARIABILITY_INDEX), Format.DECIMAL2),
    FTP_PERCENT("% FTP", Source.Karoo(Type.PERCENT_MAX_FTP, Field.PERCENT_MAX_FTP), Format.PERCENT, "POWER_3S"),
    WKG("W/KG", Source.Karoo(Type.POWER_TO_WEIGHT, Field.POWER_TO_WEIGHT), Format.WKG, "POWER_3S"),
    WORK("WORK", Source.Karoo(Type.ENERGY_OUTPUT, Field.ENERGY_OUTPUT), Format.KJ, "POWER_3S"),
    CALORIES("CALORIES", Source.Karoo(Type.CALORIES, Field.CALORIES), Format.KCAL, "POWER_3S"),
    KCAL_PER_HOUR("KCAL/H", Source.Karoo(Type.CALORIES_PER_HOUR, Field.CALORIES_PER_HOUR), Format.KCAL_H, "POWER_3S"),
    BALANCE("L/R BALANCE", Source.Karoo(Type.SMOOTHED_3S_AVERAGE_PEDAL_POWER_BALANCE, Field.PEDAL_POWER_BALANCE_LEFT), Format.BALANCE),
    PEDAL_SMOOTHNESS("PEDAL SMOOTH", Source.Karoo(Type.PEDAL_SMOOTHNESS, Field.PEDAL_SMOOTHNESS_LEFT, Field.PEDAL_SMOOTHNESS_RIGHT), Format.PERCENT),
    TORQUE_EFFECTIVENESS("TORQUE EFF", Source.Karoo(Type.TORQUE_EFFECTIVENESS, Field.TORQUE_EFFECTIVENESS_LEFT, Field.TORQUE_EFFECTIVENESS_RIGHT), Format.PERCENT),

    SPEED("SPEED", Source.Karoo(Type.SPEED, Field.SPEED), Format.SPEED, "SPEED"),
    SPEED_3S("SPEED 3S", Source.Karoo(Type.SMOOTHED_3S_AVERAGE_SPEED, Field.SMOOTHED_3S_AVERAGE_SPEED), Format.SPEED, "SPEED"),
    AVG_SPEED("AVG SPEED", Source.Karoo(Type.AVERAGE_SPEED, Field.AVERAGE_SPEED), Format.SPEED, "SPEED"),
    MAX_SPEED("MAX SPEED", Source.Karoo(Type.MAX_SPEED, Field.MAX_SPEED), Format.SPEED),

    HEART_RATE("HEART RATE", Source.Karoo(Type.HEART_RATE, Field.HEART_RATE), Format.HR, "HEART_RATE"),
    AVG_HR("AVG HR", Source.Karoo(Type.AVERAGE_HR, Field.AVG_HR), Format.HR, "HEART_RATE"),
    MAX_HR("MAX HR", Source.Karoo(Type.MAX_HR, Field.MAX_HR), Format.HR),
    HR_PERCENT_MAX("% MAX HR", Source.Karoo(Type.PERCENT_MAX_HR, Field.PERCENT_MAX_HR), Format.PERCENT, "HEART_RATE"),
    HRR_PERCENT("% HRR", Source.Karoo(Type.PERCENT_HRR, Field.PERCENT_HRR), Format.PERCENT, "HEART_RATE"),
    CORE_TEMP("CORE TEMP", Source.Karoo(Type.CORE_TEMP, Field.CORE_TEMP), Format.BODY_TEMP),

    CADENCE("CADENCE", Source.Karoo(Type.CADENCE, Field.CADENCE), Format.CADENCE, "CADENCE"),
    AVG_CADENCE("AVG CADENCE", Source.Karoo(Type.AVERAGE_CADENCE, Field.AVERAGE_CADENCE), Format.CADENCE, "CADENCE"),

    DISTANCE("DISTANCE", Source.Karoo(Type.DISTANCE, Field.DISTANCE), Format.DISTANCE, "SPEED"),
    ELAPSED_TIME("RIDE TIME", Source.Karoo(Type.ELAPSED_TIME, Field.ELAPSED_TIME), Format.DURATION, "SPEED"),
    TOTAL_TIME("TOTAL TIME", Source.Karoo(Type.RIDE_TIME, Field.RIDE_TIME), Format.DURATION, "SPEED"),
    CLOCK("TIME OF DAY", Source.Karoo(Type.CLOCK_TIME, Field.CLOCK_TIME), Format.CLOCK),
    SUNSET_IN("SUNSET IN", Source.Karoo(Type.TIME_TO_SUNSET, Field.TIME_TO_SUNSET), Format.DURATION),

    // While navigating.
    TO_GO("TO GO", Source.Karoo(Type.DISTANCE_TO_DESTINATION, Field.DISTANCE_TO_DESTINATION), Format.DISTANCE),
    ETA("ETA", Source.Karoo(Type.TIME_OF_ARRIVAL, Field.TIME_OF_ARRIVAL), Format.CLOCK),
    CLIMB_LEFT("CLIMB LEFT", Source.Karoo(Type.ELEVATION_REMAINING, Field.ASCENT_REMAINING), Format.ELEVATION),

    ELEVATION("ELEVATION", Source.Karoo(Type.PRESSURE_ELEVATION_CORRECTION, Field.PRESSURE_ELEVATION), Format.ELEVATION, "ELEVATION"),
    ASCENT("ASCENT", Source.Karoo(Type.ELEVATION_GAIN, Field.ELEVATION_GAIN), Format.ELEVATION, "ELEVATION"),
    GRADE("GRADE", Source.Karoo(Type.ELEVATION_GRADE, Field.ELEVATION_GRADE), Format.GRADE, "GRADE"),
    VAM("VAM", Source.Karoo(Type.VERTICAL_SPEED, Field.VERTICAL_SPEED), Format.VAM, "VAM"),
    TEMPERATURE("TEMP", Source.Karoo(Type.TEMPERATURE, Field.TEMPERATURE), Format.TEMP, "TEMPERATURE"),
    TYRE_FRONT("TYRE F", Source.Karoo(Type.TIRE_PRESSURE_FRONT, Field.TIRE_PRESSURE), Format.PRESSURE),
    TYRE_REAR("TYRE R", Source.Karoo(Type.TIRE_PRESSURE_REAR, Field.TIRE_PRESSURE), Format.PRESSURE),

    W_PRIME("W′ BAL", Source.Computed, Format.KJ, "W_PRIME"),
    /** Time until W' runs out at the current 3 s power; null at or below CP. */
    W_PRIME_EMPTY("W′ EMPTY IN", Source.Computed, Format.DURATION),
    /** Long-term and short-term stamina (estimates), and how long the current effort can last. */
    STAMINA("STAMINA", Source.Computed, Format.PERCENT),
    STAMINA_NOW("STAMINA NOW", Source.Computed, Format.PERCENT),
    EFFORT_LEFT("EFFORT LEFT", Source.Computed, Format.DURATION),
    /** Carbohydrate burned and eaten this ride, and eaten minus burned. */
    CARBS_BURNED("CARBS BURNED", Source.Computed, Format.GRAMS),
    CARBS_EATEN("CARBS EATEN", Source.Computed, Format.GRAMS),
    CARBS_BALANCE("CARBS +/-", Source.Computed, Format.GRAMS),
    /** Pacing on a navigated route: this stretch's target and the time ahead (+) or behind (-) the plan. */
    PACE_TARGET("PACE TARGET", Source.Computed, Format.POWER),
    PACE_AHEAD("VS PLAN", Source.Computed, Format.DELTA),
    HEADWIND("HEADWIND", Source.Computed, Format.WIND, "HEADWIND"),
    CROSSWIND("CROSSWIND", Source.Computed, Format.WIND),
    WIND_SPEED("WIND", Source.Computed, Format.WIND),

    // Used internally by the hero and the Effort/Bike panels; not offered as custom fields.
    POWER_ZONE("POWER ZONE", Source.Karoo(Type.POWER_ZONE, Field.POWER_ZONE), Format.INT, selectable = false),
    LAP_NUMBER("LAP", Source.Karoo(Type.LAP_NUMBER, Field.LAP_NUMBER), Format.INT, selectable = false),
    ;

    /** The metric drawn as this metric's mini graph, if any. */
    val graph: Metric? by lazy { graphOf?.let { valueOf(it) } }

    val karoo: Source.Karoo? get() = source as? Source.Karoo

    fun unit(u: UnitPrefs): String = when (format) {
        Format.POWER -> "W"
        Format.SPEED, Format.WIND -> Fmt.speedUnit(u)
        Format.HR -> "bpm"
        Format.CADENCE -> "rpm"
        Format.DISTANCE -> Fmt.distanceUnit(u)
        Format.ELEVATION -> Fmt.elevationUnit(u)
        Format.GRADE, Format.PERCENT -> "%"
        Format.VAM -> "m/h"
        Format.KJ -> "kJ"
        Format.KCAL -> "kcal"
        Format.KCAL_H -> "kcal/h"
        Format.GRAMS -> "g"
        Format.TEMP, Format.BODY_TEMP -> Fmt.temperatureUnit(u)
        Format.PRESSURE -> Fmt.pressureUnit(u)
        Format.WKG -> "W/kg"
        Format.DURATION, Format.DELTA, Format.CLOCK, Format.DECIMAL2, Format.INT, Format.BALANCE -> ""
    }

    fun format(v: Double?, u: UnitPrefs): String = when (format) {
        Format.POWER, Format.HR, Format.CADENCE, Format.INT, Format.KCAL, Format.KCAL_H, Format.PERCENT, Format.VAM, Format.GRAMS -> Fmt.int(v)
        Format.SPEED -> Fmt.speed(v, u)
        Format.WIND -> v?.let { Fmt.int(Fmt.speedValue(it, u)) } ?: Fmt.NONE
        Format.DISTANCE -> Fmt.distance(v, u)
        Format.DURATION -> Fmt.duration(v)
        Format.DELTA -> Fmt.delta(v)
        Format.CLOCK -> Fmt.clock(v)
        Format.ELEVATION -> Fmt.elevation(v, u)
        Format.GRADE -> Fmt.decimal(v, 1)
        Format.KJ -> if (this == W_PRIME) Fmt.decimal(v?.div(1000), 1) else Fmt.int(v)
        Format.DECIMAL2, Format.WKG -> Fmt.decimal(v, 2)
        Format.TEMP -> Fmt.temperature(v, u)
        Format.BODY_TEMP -> Fmt.temperature(v, u, digits = 1)
        Format.PRESSURE -> Fmt.pressure(v, u)
        Format.BALANCE -> Fmt.balance(v)
    }

    companion object {
        val selectableEntries: List<Metric> get() = entries.filter { it.selectable }

        /** Metrics that need the weather loaded. */
        val wind: Set<Metric> get() = setOf(HEADWIND, CROSSWIND, WIND_SPEED)

        /** Metrics the recorder keeps 1 Hz history for (mini graphs, effort bars). */
        val graphed: List<Metric> get() = entries.mapNotNull { it.graph }.distinct()
    }
}

/** Lap fields: the current lap's Karoo type next to the same value for the previous lap. */
enum class LapMetric(
    val label: String,
    val current: Source.Karoo,
    val previous: Source.Karoo,
    val format: Format,
) {
    TIME("LAP TIME", Source.Karoo(Type.ELAPSED_TIME_LAP, Field.ELAPSED_TIME), Source.Karoo(Type.ELAPSED_TIME_LAST_LAP, Field.ELAPSED_TIME), Format.DURATION),
    DISTANCE("LAP DIST", Source.Karoo(Type.DISTANCE_LAP, Field.DISTANCE), Source.Karoo(Type.DISTANCE_LAP_LAST_LAP, Field.DISTANCE), Format.DISTANCE),
    POWER("LAP POWER", Source.Karoo(Type.POWER_LAP, Field.AVERAGE_POWER), Source.Karoo(Type.AVERAGE_POWER_LAST_LAP, Field.AVERAGE_POWER), Format.POWER),
    NP("LAP NP", Source.Karoo(Type.NORMALIZED_POWER_LAP, Field.NORMALIZED_POWER), Source.Karoo(Type.NORMALIZED_POWER_LAST_LAP, Field.NORMALIZED_POWER), Format.POWER),
    MAX_POWER("LAP MAX W", Source.Karoo(Type.MAX_POWER_LAP, Field.MAX_POWER), Source.Karoo(Type.MAX_POWER_LAST_LAP, Field.MAX_POWER), Format.POWER),
    SPEED("LAP SPEED", Source.Karoo(Type.AVERAGE_SPEED_LAP, Field.AVERAGE_SPEED), Source.Karoo(Type.AVERAGE_SPEED_LAST_LAP, Field.AVERAGE_SPEED), Format.SPEED),
    HR("LAP HR", Source.Karoo(Type.AVERAGE_LAP_HR, Field.AVG_HR), Source.Karoo(Type.AVERAGE_HR_LAST_LAP, Field.AVG_HR), Format.HR),
    CADENCE("LAP CAD", Source.Karoo(Type.CADENCE_LAP, Field.AVERAGE_CADENCE), Source.Karoo(Type.AVERAGE_CADENCE_LAST_LAP, Field.AVERAGE_CADENCE), Format.CADENCE),
    ASCENT("LAP ASCENT", Source.Karoo(Type.ELEVATION_GAIN_LAP, Field.ELEVATION_GAIN), Source.Karoo(Type.ELEVATION_GAIN_LAST_LAP, Field.ELEVATION_GAIN), Format.ELEVATION),
    ;

    fun format(v: Double?, u: UnitPrefs): String = when (format) {
        Format.DURATION -> Fmt.duration(v)
        Format.DISTANCE -> Fmt.distance(v, u)
        Format.SPEED -> Fmt.speed(v, u)
        Format.ELEVATION -> Fmt.elevation(v, u)
        else -> Fmt.int(v)
    }

    fun unit(u: UnitPrefs): String = when (format) {
        Format.POWER -> "W"
        Format.SPEED -> Fmt.speedUnit(u)
        Format.HR -> "bpm"
        Format.CADENCE -> "rpm"
        Format.DISTANCE -> Fmt.distanceUnit(u)
        Format.ELEVATION -> Fmt.elevationUnit(u)
        else -> ""
    }
}
