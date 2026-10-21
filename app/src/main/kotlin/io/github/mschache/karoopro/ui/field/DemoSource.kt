package io.github.mschache.karoopro.ui.field

import io.github.mschache.karoopro.calc.Gearing
import io.github.mschache.karoopro.calc.ProfilePoint
import io.github.mschache.karoopro.calc.TargetBand
import io.github.mschache.karoopro.calc.TargetKind
import io.github.mschache.karoopro.calc.WorkoutState
import io.github.mschache.karoopro.data.LapMetric
import io.github.mschache.karoopro.data.Metric
import io.github.mschache.karoopro.data.Profile
import io.github.mschache.karoopro.data.Settings
import io.github.mschache.karoopro.data.Source
import io.github.mschache.karoopro.data.WorkoutOverlay
import io.github.mschache.karoopro.weather.HourForecast
import io.github.mschache.karoopro.weather.WeatherData
import io.hammerhead.karooext.models.BatteryStatus
import io.hammerhead.karooext.models.DataType
import kotlin.math.PI
import kotlin.math.sin

/**
 * A believable mid-ride snapshot for the page editor and the in-app preview. [t] (seconds) moves the
 * numbers along so the preview shows zone colors changing.
 */
class DemoSource(
    override val settings: Settings,
    profile: Profile,
    private val t: Long = 0,
    private val noPowerMeter: Boolean = false,
    private val overlayMode: Int = 0,
) : RideSource {
    override val profile: Profile = profile.takeIf { it.powerZones.isNotEmpty() } ?: Profile.DEFAULT
    private val ftp = this.profile.ftp.coerceAtLeast(100).toDouble()

    private fun wave(period: Double, phase: Double = 0.0) = sin(2 * PI * (t / period) + phase)

    private val power = ftp * (1.0 + 0.3 * wave(40.0))
    private val hr = 158 + 12 * wave(60.0, 1.0)
    private val speed = 9.4 + 1.2 * wave(50.0, 2.0)

    override val powerAvailable get() = !noPowerMeter
    override val hrAvailable get() = true
    override val shiftingAvailable get() = true
    override val workoutActive get() = true

    private val values: Map<Metric, Double> = mapOf(
        Metric.POWER to power,
        Metric.POWER_3S to power,
        Metric.POWER_10S to power * 0.98,
        Metric.POWER_30S to power * 0.95,
        Metric.AVG_POWER to ftp * 0.78,
        Metric.MAX_POWER to ftp * 3.4,
        Metric.NP to ftp * 0.86,
        Metric.IF to 0.86,
        Metric.TSS to 74.0,
        Metric.FTP_PERCENT to power / ftp * 100,
        Metric.WKG to power / 72,
        Metric.WORK to 1480.0,
        Metric.CALORIES to 1415.0,
        Metric.BALANCE to 48.0,
        Metric.SPEED to speed,
        Metric.SPEED_3S to speed,
        Metric.AVG_SPEED to 8.6,
        Metric.MAX_SPEED to 17.2,
        Metric.HEART_RATE to hr,
        Metric.AVG_HR to 146.0,
        Metric.MAX_HR to 181.0,
        Metric.HR_PERCENT_MAX to hr / 190 * 100,
        Metric.CADENCE to 91 + 4 * wave(20.0),
        Metric.AVG_CADENCE to 88.0,
        Metric.DISTANCE to 48_250.0,
        Metric.ELAPSED_TIME to 5_833_000.0 + t * 1000,
        Metric.TOTAL_TIME to 6_100_000.0 + t * 1000,
        Metric.CLOCK to 39_600_000.0,
        Metric.ELEVATION to 412.0,
        Metric.ASCENT to 864.0,
        Metric.GRADE to 5.4 + wave(30.0),
        Metric.VAM to 1080.0,
        Metric.TEMPERATURE to 19.0,
        Metric.W_PRIME to 13_400.0,
        Metric.HEADWIND to -2.6,
        Metric.WIND_SPEED to 4.2,
        Metric.POWER_ZONE to 4.0,
        Metric.LAP_NUMBER to 4.0,
    ).mapValues { it.value }

    override fun value(metric: Metric): Double? {
        if (noPowerMeter && metric.name.contains("POWER") || noPowerMeter && metric in setOf(Metric.NP, Metric.IF, Metric.TSS, Metric.WKG, Metric.FTP_PERCENT, Metric.WORK, Metric.W_PRIME)) return null
        return values[metric]
    }

    private val lapValues: Map<String, Double> = buildMap {
        fun put(lm: LapMetric, cur: Double, prev: Double) {
            put(lm.current.typeId, cur)
            put(lm.previous.typeId, prev)
        }
        put(LapMetric.TIME, 512_000.0, 600_000.0)
        put(LapMetric.DISTANCE, 4_800.0, 5_000.0)
        put(LapMetric.POWER, ftp * 0.92, ftp * 0.88)
        put(LapMetric.NP, ftp * 0.95, ftp * 0.93)
        put(LapMetric.MAX_POWER, ftp * 2.1, ftp * 2.4)
        put(LapMetric.SPEED, 9.4, 8.33)
        put(LapMetric.HR, 161.0, 163.0)
        put(LapMetric.CADENCE, 90.0, 90.0)
        put(LapMetric.ASCENT, 120.0, 95.0)
        put(DataType.Type.SHIFTING_COUNT, 385.0)
    }

    override fun value(source: Source.Karoo): Double? =
        lapValues[source.typeId] ?: Metric.entries.firstOrNull { it.karoo == source }?.let { value(it) }

    override fun history(metric: Metric, seconds: Int): List<Float?> {
        val base = values[metric] ?: return emptyList()
        if (noPowerMeter && metric.name.contains("POWER")) return emptyList()
        return (0 until seconds).map { i ->
            val x = (i + t).toDouble()
            (base * (1 + 0.18 * sin(x / 23.0) + 0.09 * sin(x / 7.0) + if ((i / 60) % 3 == 1) 0.25 else 0.0)).toFloat()
        }
    }

    override fun best(seconds: Int): Double? = when (seconds) {
        5 -> ftp * 3.2
        60 -> ftp * 1.8
        300 -> ftp * 1.22
        1200 -> ftp * 1.02
        else -> null
    }.takeUnless { noPowerMeter }

    override val wPrimeBalance get() = if (noPowerMeter) null else 13_400.0
    override val wPrimeMax get() = settings.wPrimeJoules.toDouble()
    override val powerZoneSeconds get() = if (noPowerMeter) LongArray(7) else longArrayOf(820, 1640, 1210, 980, 610, 240, 60)
    override val hrZoneSeconds get() = longArrayOf(400, 1900, 2100, 1050, 210)

    override fun terrainBehind(meters: Double): List<ProfilePoint> = (0..100).map { i ->
        val d = 48_250.0 - meters + i * meters / 100
        ProfilePoint(d, 300 + 110 * (i / 100.0) + 18 * sin(i / 9.0))
    }

    override fun terrainAhead(meters: Double): List<ProfilePoint> = (0..100).map { i ->
        val d = i * meters / 100
        ProfilePoint(d, 412 + 90 * sin(i / 32.0) - 40 * (i / 100.0))
    }

    override val workout: WorkoutState
        get() {
            val bounds = TargetBand.bounds(TargetKind.POWER, null, ftp * 1.06, ftp * 1.16)!!
            val left = (214 - t % 214).toInt()
            val out = power.toInt()
            return WorkoutState(
                stepNumber = 2, stepCount = 8, kind = TargetKind.POWER,
                low = bounds.first, high = bounds.second, output = out,
                status = TargetBand.status(out, bounds),
                stepRemainingS = left, totalRemainingS = 1_860, stepElapsedS = 300 - left,
                stepAverage = (ftp * 1.1).toInt(), inTargetPercent = 83,
                progress = (300 - left) / 300f,
            )
        }

    override val overlay: WorkoutOverlay?
        get() = when (overlayMode) {
            1 -> WorkoutOverlay.NextStep(3, 8, 5)
            2 -> WorkoutOverlay.NewTarget(workout.copy(stepNumber = 3))
            else -> null
        }

    override val weather = WeatherData(
        fetchedAt = 0, latitude = 0.0, longitude = 0.0,
        windSpeed = 5.0, windFromDeg = 200.0, gust = 8.3, temperature = 19.0, rainChance = 20,
        hours = (1..6).map { i ->
            HourForecast(1_760_000_000L + i * 3600, 4.0 + i * 0.4, 190.0 + i * 12, 7.0, 10 * i, 19.0 - i)
        },
    )

    override val weatherStatus = io.github.mschache.karoopro.weather.WeatherStatus.OK

    override val heading = 20.0 + 25 * sin(t / 45.0)

    override val gearing = Gearing(
        frontGear = 2, frontMax = 2, rearGear = (4 + (t / 8) % 8).toInt(), rearMax = 12,
        frontTeeth = 50, rearTeeth = listOf(34, 30, 27, 24, 21, 19, 17, 15, 14, 13, 12, 11)[(4 + (t / 8) % 8).toInt() - 1],
        frontTeethList = listOf(34, 50), rearTeethList = listOf(11, 12, 13, 14, 15, 17, 19, 21, 24, 27, 30, 34),
    )

    override val batteries = Batteries(frontDerailleur = BatteryStatus.GOOD, rearDerailleur = BatteryStatus.OK, powerMeter = BatteryStatus.LOW)

    override val nowMs = 0L
}
