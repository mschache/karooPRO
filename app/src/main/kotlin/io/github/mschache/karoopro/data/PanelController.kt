package io.github.mschache.karoopro.data

import io.github.mschache.karoopro.calc.ClimbProgress
import io.github.mschache.karoopro.calc.SegmentState
import io.github.mschache.karoopro.coach.CoachInbox
import io.github.mschache.karoopro.coach.CoachLink
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
 * actions, and switched automatically when a workout starts, Di2/AXS shifting appears or a coach
 * message arrives. During a workout, a Coach panel that opened by itself goes back after 20 s
 * without a tap, so coach messages never hide the target for long. The Climb panel opens just
 * before each climb on a navigated route and the Segment panel for a Strava Live Segment; both give
 * the screen back when it's over, unless the rider took over.
 */
class PanelController(
    private val hub: RideDataHub,
    private val workout: WorkoutMonitor,
    private val coach: CoachLink,
    private val settings: StateFlow<Settings>,
    private val scope: CoroutineScope,
    /** The climb the rider is on or the next one on the navigated route. */
    private val climb: () -> ClimbProgress? = { null },
    /** The Strava Live Segment coming up, running or just finished. */
    private val segment: () -> SegmentState? = { null },
    /** A route with an elevation profile is navigated, so the Pace panel has a plan. */
    private val paceAvailable: () -> Boolean = { false },
    /** A climb or segment finished this ride. */
    private val hasResults: () -> Boolean = { false },
    /** A tap in the middle of the Fuel panel logs what the rider ate. */
    private val onFuelTap: () -> Unit = {},
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

    /** The panel a coach message replaced; confirming the message goes back to it. */
    @Volatile private var beforeCoach: Panel? = null

    /** When a coach message last opened the Coach panel by itself; 0 once the rider took over. */
    @Volatile private var coachOpenedAt = 0L

    /**
     * A panel that opens by itself while it's wanted and, when that ends, goes back to the panel it
     * replaced, unless the rider flipped panels in between.
     */
    private inner class AutoPanel(val panel: Panel) {
        @Volatile var before: Panel? = null
        private var was = false

        fun tick(wanted: Boolean, open: Boolean) {
            if (wanted && !was && open && shown() != panel) {
                val from = shown()
                show(panel)
                before = from
            } else if (!wanted && was) {
                before?.takeIf { shown() == panel && it in visiblePanels() }?.let { show(it) }
                before = null
            }
            was = wanted
        }
    }

    private val climbPanel = AutoPanel(Panel.CLIMB)
    private val segmentPanel = AutoPanel(Panel.SEGMENT)
    private val fuelPanel = AutoPanel(Panel.FUEL)

    /** Until when a fuel reminder shows the Fuel panel. */
    @Volatile private var fuelUntil = 0L

    /** Shows the Fuel panel for [FUEL_SHOW_MS] after an eat or drink reminder. */
    fun fuelReminder() {
        fuelUntil = System.currentTimeMillis() + FUEL_SHOW_MS
    }

    /** Set by the 1 s loop: a climb is ahead on the navigated route; a segment is near, running or just done. */
    @Volatile private var climbAhead = false
    @Volatile private var segmentNear = false

    private fun riderTookOver() {
        beforeCoach = null
        coachOpenedAt = 0
        climbPanel.before = null
        segmentPanel.before = null
        fuelPanel.before = null
    }

    fun visiblePanels(s: Settings = settings.value): List<Panel> =
        s.orderedPanels.filter { it.enabled }.map { it.panel }.filter {
            demoMode || when (it) {
                Panel.WORKOUT -> workout.active
                Panel.BIKE -> shiftingAvailable
                Panel.COACH -> coach.available
                Panel.CLIMB -> climbAhead
                Panel.SEGMENT -> segmentNear
                Panel.PACE -> paceAvailable()
                Panel.RESULTS -> hasResults()
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
        riderTookOver()
        _current.value = visible[(i + direction).mod(visible.size)]
        invalidate()
    }

    /**
     * A tap on the lower half, [direction] -1/0/+1 for its left, middle and right third. On the
     * Coach panel with an unread message any tap confirms it (and returns to the panel the message
     * replaced); otherwise the sides flip panels.
     */
    fun tap(direction: Int) {
        coachOpenedAt = 0
        climbPanel.before = null
        segmentPanel.before = null
        fuelPanel.before = null
        if (direction == 0 && shown() == Panel.FUEL) {
            onFuelTap()
            invalidate()
        } else if (shown() == Panel.COACH && coach.state.value?.unread == true) {
            coach.acknowledge()
            beforeCoach?.takeIf { it in visiblePanels() }?.let { _current.value = it }
            beforeCoach = null
            invalidate()
        } else if (direction != 0) {
            step(direction)
        }
    }

    fun show(panel: Panel) {
        riderTookOver()
        _current.value = panel
        invalidate()
    }

    fun invalidate() {
        _invalidations.tryEmit(Unit)
    }

    fun start() {
        hub.keepAlive(listOf(DataType.Type.SHIFTING_GEARS))
        scope.launch {
            val inbox = CoachInbox()
            coach.state.collect { info ->
                val s = settings.value
                val enabled = s.orderedPanels.any { it.panel == Panel.COACH && it.enabled }
                val now = System.currentTimeMillis()
                if (inbox.isNew(info, now) && s.autoOpenCoach && enabled) {
                    // A Coach panel the rider chose stays; one opened by a message gets the timer.
                    if (shown() != Panel.COACH) {
                        beforeCoach = shown()
                        coachOpenedAt = now
                    } else if (coachOpenedAt > 0) {
                        coachOpenedAt = now
                    }
                    _current.value = Panel.COACH
                }
                invalidate()
            }
        }
        scope.launch {
            var wasWorkout = false
            var wasShifting = false
            while (isActive) {
                val c = climb()
                climbAhead = c != null
                segmentNear = segment() != null
                shiftingAvailable = hub.stream(DataType.Type.SHIFTING_GEARS).value.field(DataType.Field.SHIFTING_REAR_GEAR) != null
                val s = settings.value
                val enabled = s.orderedPanels.filter { it.enabled }.map { it.panel }
                val isWorkout = workout.active
                if (isWorkout && !wasWorkout && s.autoOpenWorkout && Panel.WORKOUT in enabled) show(Panel.WORKOUT)
                if (shiftingAvailable && !wasShifting && !isWorkout && s.autoOpenBike && Panel.BIKE in enabled) show(Panel.BIKE)
                // Climbs open 200 m before they start and close at the top; segments for as long as they're near.
                climbPanel.tick(c != null && c.toStart <= CLIMB_OPEN_BEFORE_M, !isWorkout && s.autoOpenClimb && Panel.CLIMB in enabled)
                segmentPanel.tick(segmentNear, !isWorkout && s.autoOpenSegment && Panel.SEGMENT in enabled)
                fuelPanel.tick(System.currentTimeMillis() < fuelUntil, !isWorkout && Panel.FUEL in enabled)
                if (shown() != Panel.COACH) {
                    coachOpenedAt = 0
                } else if (isWorkout && coachOpenedAt > 0 && System.currentTimeMillis() - coachOpenedAt >= COACH_RETURN_MS) {
                    val visible = visiblePanels()
                    val back = listOfNotNull(beforeCoach, Panel.WORKOUT).firstOrNull { it in visible }
                    if (back != null) show(back) else coachOpenedAt = 0
                }
                wasWorkout = isWorkout
                wasShifting = shiftingAvailable
                delay(1000)
            }
        }
    }

    private companion object {
        const val COACH_RETURN_MS = 20_000L
        const val CLIMB_OPEN_BEFORE_M = 200.0
        const val FUEL_SHOW_MS = 20_000L
    }
}
