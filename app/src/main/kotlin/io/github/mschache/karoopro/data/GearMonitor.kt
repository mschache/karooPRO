package io.github.mschache.karoopro.data

import io.github.mschache.karoopro.alerts.AlertQueue
import io.github.mschache.karoopro.alerts.Beep
import io.github.mschache.karoopro.calc.GearLimit
import io.github.mschache.karoopro.calc.GearLimits
import io.github.mschache.karoopro.calc.Gearing
import io.github.mschache.karoopro.karoo.field
import io.hammerhead.karooext.models.DataType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Reads the Di2/AXS gearing, and beeps when a shift lands on either end of the cassette. */
class GearMonitor(
    private val hub: RideDataHub,
    private val settings: StateFlow<Settings>,
    private val alerts: AlertQueue,
    private val scope: CoroutineScope,
) {
    /** The current gearing from the shifting streams and the saved shifter's teeth; null without shifting data. */
    fun gearing(): Gearing? {
        val s = hub.stream(DataType.Type.SHIFTING_GEARS).value
        val front = s.field(DataType.Field.SHIFTING_FRONT_GEAR) ?: return null
        val shifter = hub.savedDevices.value?.devices?.firstOrNull { it.gearInfo != null }?.gearInfo
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

    fun start() {
        scope.launch {
            var last: Gearing? = null
            while (isActive) {
                val now = if (hub.isRecording && settings.value.gearLimitBeep) gearing() else null
                when (GearLimits.reached(last, now)) {
                    GearLimit.TOP -> alerts.beep(Beep.GEAR_TOP)
                    GearLimit.BOTTOM -> alerts.beep(Beep.GEAR_BOTTOM)
                    null -> Unit
                }
                last = now
                delay(TICK_MS)
            }
        }
    }

    private companion object {
        const val TICK_MS = 200L
    }
}
