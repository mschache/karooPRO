package io.github.mschache.karoopro.ui.field

import io.github.mschache.karoopro.AppGraph
import io.github.mschache.karoopro.calc.Gearing
import io.github.mschache.karoopro.calc.Polyline
import io.github.mschache.karoopro.calc.ProfilePoint
import io.github.mschache.karoopro.calc.WorkoutState
import io.github.mschache.karoopro.coach.CoachInfo
import io.github.mschache.karoopro.data.Metric
import io.github.mschache.karoopro.data.Profile
import io.github.mschache.karoopro.data.Settings
import io.github.mschache.karoopro.data.Source
import io.github.mschache.karoopro.data.WorkoutOverlay
import io.github.mschache.karoopro.karoo.field
import io.github.mschache.karoopro.weather.WeatherData
import io.hammerhead.karooext.models.BatteryStatus
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.OnNavigationState

data class Batteries(
    val frontDerailleur: BatteryStatus? = null,
    val rearDerailleur: BatteryStatus? = null,
    val shifting: BatteryStatus? = null,
    val powerMeter: BatteryStatus? = null,
)

/** Everything the field can draw, from the live Karoo or from demo data. */
interface RideSource {
    val profile: Profile
    val settings: Settings
    val powerAvailable: Boolean
    val hrAvailable: Boolean
    val shiftingAvailable: Boolean
    val workoutActive: Boolean
    fun value(metric: Metric): Double?
    fun value(source: Source.Karoo): Double?
    /** Newest [seconds] of 1 Hz history, oldest first; null entries are gaps. */
    fun history(metric: Metric, seconds: Int): List<Float?>
    fun best(seconds: Int): Double?
    val wPrimeBalance: Double?
    val wPrimeMax: Double
    val powerZoneSeconds: LongArray
    val hrZoneSeconds: LongArray
    /** Profile behind the rider within [meters]. */
    fun terrainBehind(meters: Double): List<ProfilePoint>
    /** Route profile ahead within [meters], distances relative to the rider; empty when not navigating. */
    fun terrainAhead(meters: Double): List<ProfilePoint>
    val workout: WorkoutState?
    val overlay: WorkoutOverlay?
    val weather: WeatherData?
    val weatherStatus: io.github.mschache.karoopro.weather.WeatherStatus
    val heading: Double?
    val gearing: Gearing?
    val batteries: Batteries
    /** Karoo Coach's state; null when it isn't installed. */
    val coach: CoachInfo?
    val nowMs: Long
}

/** Reads the live hub, recorder, workout monitor and weather. */
class LiveSource(private val g: AppGraph) : RideSource {
    override val profile get() = g.hub.profile.value
    override val settings get() = g.settings.value
    override val powerAvailable get() = g.hub.isStreaming(DataType.Type.SMOOTHED_3S_AVERAGE_POWER) || g.hub.isStreaming(DataType.Type.POWER)
    override val hrAvailable get() = g.hub.isStreaming(DataType.Type.HEART_RATE)
    override val shiftingAvailable get() = g.panels.shiftingAvailable
    override val workoutActive get() = g.workout.active
    override fun value(metric: Metric) = g.hub.value(metric)
    override fun value(source: Source.Karoo) = g.hub.value(source)
    override fun history(metric: Metric, seconds: Int): List<Float?> = g.recorder.read {
        history[metric]?.last(seconds)?.map { if (it.isNaN()) null else it.toFloat() }.orEmpty()
    }
    override fun best(seconds: Int) = g.recorder.read { best.bestFor(seconds) }
    override val wPrimeBalance get() = g.hub.value(Metric.W_PRIME)
    override val wPrimeMax get() = g.settings.value.wPrimeJoules.toDouble()
    override val powerZoneSeconds get() = g.recorder.read { powerZoneTime.seconds.copyOf() }
    override val hrZoneSeconds get() = g.recorder.read { hrZoneTime.seconds.copyOf() }
    override fun terrainBehind(meters: Double) = g.recorder.read { terrain.window(meters) }

    private var routeCache: Pair<String, List<ProfilePoint>>? = null

    override fun terrainAhead(meters: Double): List<ProfilePoint> {
        val nav = g.hub.navigation.value as? OnNavigationState.NavigationState.NavigatingRoute ?: return emptyList()
        val encoded = nav.routeElevationPolyline ?: return emptyList()
        val route = routeCache?.takeIf { it.first == encoded }?.second
            ?: Polyline.decode(encoded, 1).map { ProfilePoint(it.first, it.second) }.also { routeCache = encoded to it }
        val toGo = g.hub.stream(DataType.Type.DISTANCE_TO_DESTINATION).value.field(DataType.Field.DISTANCE_TO_DESTINATION) ?: return emptyList()
        val along = nav.routeDistance - toGo
        return route.filter { it.distance >= along && it.distance <= along + meters }
            .map { ProfilePoint(it.distance - along, it.elevation) }
    }

    override val workout get() = g.workout.state.value
    override val overlay get() = g.workout.overlay.value
    override val weather get() = g.weather.weather.value
    override val weatherStatus get() = g.weather.status.value
    override val heading get() = g.weather.heading
    override val gearing: Gearing?
        get() {
            val s = g.hub.stream(DataType.Type.SHIFTING_GEARS).value
            val front = s.field(DataType.Field.SHIFTING_FRONT_GEAR) ?: return null
            val shifter = g.hub.savedDevices.value?.devices?.firstOrNull { it.gearInfo != null }?.gearInfo
            return Gearing(
                frontGear = front.toInt(),
                frontMax = s.field(DataType.Field.SHIFTING_FRONT_GEAR_MAX)?.toInt() ?: shifter?.maxFrontGears,
                rearGear = s.field(DataType.Field.SHIFTING_REAR_GEAR)?.toInt(),
                rearMax = s.field(DataType.Field.SHIFTING_REAR_GEAR_MAX)?.toInt() ?: shifter?.maxRearGears,
                frontTeeth = s.field(DataType.Field.SHIFTING_FRONT_GEAR_TEETH)?.toInt()?.takeIf { it > 0 },
                rearTeeth = s.field(DataType.Field.SHIFTING_REAR_GEAR_TEETH)?.toInt()?.takeIf { it > 0 },
                frontTeethList = shifter?.frontTeeth,
                rearTeethList = shifter?.rearTeeth,
            )
        }
    override val batteries: Batteries
        get() {
            val s = g.hub.stream(DataType.Type.SHIFTING_BATTERY).value
            fun status(field: String) = s.field(field)?.toInt()?.let { BatteryStatus.entries.getOrNull(it) }
            val powerMeter = g.hub.savedDevices.value?.devices
                ?.firstOrNull { it.enabled && DataType.Source.POWER in it.supportedDataTypes }
                ?.details?.lastBattery
            return Batteries(
                frontDerailleur = status(DataType.Field.SHIFTING_BATTERY_STATUS_FRONT_DERAILLEUR),
                rearDerailleur = status(DataType.Field.SHIFTING_BATTERY_STATUS_REAR_DERAILLEUR),
                shifting = status(DataType.Field.SHIFTING_BATTERY_STATUS),
                powerMeter = powerMeter,
            )
        }
    override val coach get() = g.coach.state.value
    override val nowMs get() = System.currentTimeMillis()
}
