package io.github.mschache.karoopro.data

import io.github.mschache.karoopro.alerts.AlertQueue
import io.github.mschache.karoopro.alerts.Beep
import io.github.mschache.karoopro.calc.TargetKind
import io.github.mschache.karoopro.calc.StepSummary
import io.github.mschache.karoopro.calc.WorkoutEvent
import io.github.mschache.karoopro.calc.WorkoutSample
import io.github.mschache.karoopro.calc.WorkoutState
import io.github.mschache.karoopro.calc.WorkoutTracker
import io.github.mschache.karoopro.coach.PlanStep
import io.github.mschache.karoopro.coach.PlanText
import io.github.mschache.karoopro.karoo.field
import io.hammerhead.karooext.models.DataType.Field
import io.hammerhead.karooext.models.DataType.Type
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** What the field shows on top of everything while a step change is coming or just happened. */
sealed class WorkoutOverlay {
    /** [plan] is the planned step, when KarooCoach's relay knows the workout. */
    data class NextStep(val nextStep: Int, val stepCount: Int, val secondsLeft: Int, val plan: PlanStep? = null) : WorkoutOverlay()
    data class NewTarget(val state: WorkoutState) : WorkoutOverlay()
}

/**
 * Follows the Karoo's structured workout while a ride is active, keeps the Workout panel state
 * and fires the step-change beeps and pop-ups whether or not a KarooPro field is on screen.
 */
class WorkoutMonitor(
    private val hub: RideDataHub,
    private val settings: StateFlow<Settings>,
    private val alerts: AlertQueue,
    private val scope: CoroutineScope,
    /** True when a KarooPro field is on the visible ride page. */
    private val fieldVisible: () -> Boolean,
    /** The planned step after the running one, from KarooCoach; null when unknown. */
    private val plannedNext: (WorkoutState, Long) -> PlanStep? = { _, _ -> null },
) {
    private val tracker = WorkoutTracker()
    private val _state = MutableStateFlow<WorkoutState?>(null)
    val state: StateFlow<WorkoutState?> = _state
    private val _lastFinished = MutableStateFlow<StepSummary?>(null)
    /** The step that ended last in this ride's running or just-finished workout. */
    val lastFinished: StateFlow<StepSummary?> = _lastFinished
    private val _overlay = MutableStateFlow<WorkoutOverlay?>(null)
    val overlay: StateFlow<WorkoutOverlay?> = _overlay
    private val _next = MutableStateFlow<PlanStep?>(null)
    /** The next step's duration and target, when KarooCoach's relay matched the workout to a plan. */
    val next: StateFlow<PlanStep?> = _next
    private var newTargetUntil = 0L

    val active: Boolean get() = _state.value != null

    fun start() {
        scope.launch {
            var gate: Job? = null
            var detail: Job? = null
            while (isActive) {
                if (hub.isRideActive) {
                    if (gate == null) gate = hub.keepAlive(listOf(Type.WORKOUT_INTERVAL_COUNT))
                    val stepCount = hub.stream(Type.WORKOUT_INTERVAL_COUNT).value.field(Field.WORKOUT_STEP_COUNT)?.toInt() ?: 0
                    if (stepCount > 0 && detail == null) detail = hub.keepAlive(DETAIL_STREAMS)
                    if (stepCount <= 0) {
                        detail?.cancel()
                        detail = null
                    }
                    if (hub.isRecording) {
                        val now = System.currentTimeMillis()
                        val events = tracker.update(if (stepCount > 0) sample(stepCount) else null, now)
                        _next.value = tracker.state?.takeUnless { it.isLastStep }?.let { plannedNext(it, now) }
                        handle(events, now)
                        _state.value = tracker.state
                        _lastFinished.value = tracker.lastFinished
                        _overlay.value = overlayFor(tracker.state, now)
                    }
                } else {
                    gate?.cancel()
                    detail?.cancel()
                    gate = null
                    detail = null
                    if (_state.value != null) tracker.reset()
                    _state.value = null
                    _lastFinished.value = null
                    _overlay.value = null
                    _next.value = null
                }
                delay(TICK_MS)
            }
        }
    }

    private fun sample(stepCount: Int): WorkoutSample {
        fun v(type: String, field: String) = hub.stream(type).value.field(field)
        val powerTarget = v(Type.WORKOUT_POWER_TARGET, Field.WORKOUT_TARGET_VALUE)?.takeIf { it > 0 }
            ?: v(Type.WORKOUT_POWER_TARGET, Field.WORKOUT_TARGET_MAX_VALUE)?.takeIf { it > 0 }
        val hrTarget = v(Type.WORKOUT_HEART_RATE_TARGET, Field.WORKOUT_TARGET_VALUE)?.takeIf { it > 0 }
            ?: v(Type.WORKOUT_HEART_RATE_TARGET, Field.WORKOUT_TARGET_MAX_VALUE)?.takeIf { it > 0 }
        val cadTarget = v(Type.WORKOUT_CADENCE_TARGET, Field.WORKOUT_TARGET_VALUE)?.takeIf { it > 0 }
        val (kind, targetType) = when {
            powerTarget != null -> TargetKind.POWER to Type.WORKOUT_POWER_TARGET
            hrTarget != null -> TargetKind.HEART_RATE to Type.WORKOUT_HEART_RATE_TARGET
            cadTarget != null -> TargetKind.CADENCE to Type.WORKOUT_CADENCE_TARGET
            else -> TargetKind.NONE to Type.WORKOUT_PRIMARY_TARGET
        }
        val output = when (kind) {
            TargetKind.POWER -> hub.value(Metric.POWER_3S)
            TargetKind.HEART_RATE -> hub.value(Metric.HEART_RATE)
            TargetKind.CADENCE -> hub.value(Metric.CADENCE)
            TargetKind.NONE -> null
        }
        return WorkoutSample(
            stepIndex = v(Type.WORKOUT_INTERVAL_COUNT, Field.WORKOUT_CURRENT_STEP)?.toInt(),
            stepCount = stepCount,
            stepRemainingMs = v(Type.WORKOUT_REMAINING_INTERVAL_DURATION, Field.WORKOUT_TIME_TO_STEP_FINISH)?.toLong(),
            totalRemainingMs = v(Type.WORKOUT_REMAINING_TOTAL_DURATION, Field.WORKOUT_REMAINING_TIME)?.toLong(),
            kind = kind,
            targetValue = v(targetType, Field.WORKOUT_TARGET_VALUE),
            targetMin = v(targetType, Field.WORKOUT_TARGET_MIN_VALUE),
            targetMax = v(targetType, Field.WORKOUT_TARGET_MAX_VALUE),
            output = output,
        )
    }

    private fun handle(events: List<WorkoutEvent>, now: Long) {
        val s = settings.value
        val popupWanted = s.systemAlert && !fieldVisible()
        for (e in events) when (e) {
            is WorkoutEvent.Countdown -> {
                if (e.secondsLeft >= 10) {
                    if (s.alertBeep) alerts.beep(Beep.NEXT_SOON)
                    if (popupWanted) {
                        val title = _next.value?.let { "Next: ${PlanText.summary(it)}" } ?: "Next: step ${e.nextStepNumber}/${e.stepCount}"
                        alerts.popup("karoopro-next", title, "in ${e.secondsLeft} s", 9_000)
                    }
                } else if (s.countdownBeeps) {
                    alerts.beep(Beep.TICK)
                }
            }
            is WorkoutEvent.StepStarted -> {
                newTargetUntil = now + NEW_TARGET_MS
                if (s.alertBeep) alerts.beep(Beep.STEP_START)
                if (popupWanted) {
                    val st = e.state
                    val target = if (st.low != null && st.high != null) "${st.low}–${st.high} ${st.kind.unit}" else st.kind.label
                    alerts.popup("karoopro-step", "Step ${st.stepNumber}/${st.stepCount}", target, NEW_TARGET_MS)
                }
            }
            WorkoutEvent.Finished -> newTargetUntil = 0
        }
    }

    private fun overlayFor(state: WorkoutState?, now: Long): WorkoutOverlay? {
        if (state == null) return null
        val left = state.stepRemainingS
        return when {
            left != null && left in 1..10 && !state.isLastStep ->
                WorkoutOverlay.NextStep(state.stepNumber + 1, state.stepCount, left, _next.value)
            now < newTargetUntil -> WorkoutOverlay.NewTarget(state)
            else -> null
        }
    }

    companion object {
        private const val TICK_MS = 250L
        private const val NEW_TARGET_MS = 5_000L
        val DETAIL_STREAMS = listOf(
            Type.WORKOUT_REMAINING_INTERVAL_DURATION,
            Type.WORKOUT_REMAINING_TOTAL_DURATION,
            Type.WORKOUT_POWER_TARGET,
            Type.WORKOUT_HEART_RATE_TARGET,
            Type.WORKOUT_CADENCE_TARGET,
            Type.WORKOUT_PRIMARY_TARGET,
            Type.SMOOTHED_3S_AVERAGE_POWER,
            Type.HEART_RATE,
            Type.CADENCE,
        )
    }
}
