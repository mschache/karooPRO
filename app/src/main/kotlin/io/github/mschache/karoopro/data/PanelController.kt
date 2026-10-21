package io.github.mschache.karoopro.data

import io.github.mschache.karoopro.karoo.field
import io.hammerhead.karooext.models.DataType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Which panel the field shows. Shared by every KarooPRO field instance; flipped by taps and bonus
 * actions, and switched automatically when a workout starts or Di2/AXS shifting appears.
 */
class PanelController(
    private val hub: RideDataHub,
    private val workout: WorkoutMonitor,
    private val settings: StateFlow<Settings>,
    private val scope: CoroutineScope,
) {
    private val _current = MutableStateFlow(Panel.RIDE)
    val current: StateFlow<Panel> = _current

    private val _invalidations = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** Fires when a tap changed something, so fields redraw without waiting for their next tick. */
    val invalidations: SharedFlow<Unit> = _invalidations

    @Volatile var shiftingAvailable = false
        private set

    /** Set while the in-app preview runs: every enabled panel is shown, live or not. */
    @Volatile var demoMode = false

    fun visiblePanels(s: Settings = settings.value): List<Panel> =
        s.orderedPanels.filter { it.enabled }.map { it.panel }.filter {
            demoMode || when (it) {
                Panel.WORKOUT -> workout.active
                Panel.BIKE -> shiftingAvailable
                else -> true
            }
        }.ifEmpty { listOf(Panel.RIDE) }

    /** The panel to draw: the selected one if visible, else the first visible one. */
    fun shown(): Panel {
        val visible = visiblePanels()
        return _current.value.takeIf { it in visible } ?: visible.first()
    }

    fun step(direction: Int) {
        val visible = visiblePanels()
        val i = visible.indexOf(shown()).coerceAtLeast(0)
        _current.value = visible[(i + direction).mod(visible.size)]
        invalidate()
    }

    fun show(panel: Panel) {
        _current.value = panel
        invalidate()
    }

    fun invalidate() {
        _invalidations.tryEmit(Unit)
    }

    fun start() {
        hub.keepAlive(listOf(DataType.Type.SHIFTING_GEARS))
        scope.launch {
            var wasWorkout = false
            var wasShifting = false
            while (isActive) {
                shiftingAvailable = hub.stream(DataType.Type.SHIFTING_GEARS).value.field(DataType.Field.SHIFTING_REAR_GEAR) != null
                val s = settings.value
                val enabled = s.orderedPanels.filter { it.enabled }.map { it.panel }
                val isWorkout = workout.active
                if (isWorkout && !wasWorkout && s.autoOpenWorkout && Panel.WORKOUT in enabled) show(Panel.WORKOUT)
                if (shiftingAvailable && !wasShifting && !isWorkout && s.autoOpenBike && Panel.BIKE in enabled) show(Panel.BIKE)
                wasWorkout = isWorkout
                wasShifting = shiftingAvailable
                delay(1000)
            }
        }
    }
}
