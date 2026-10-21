package io.github.mschache.karoopro.calc

import kotlin.math.ceil
import kotlin.math.roundToInt

enum class TargetKind(val label: String, val unit: String, val impliedBand: Double) {
    POWER("POWER", "W", 0.05),
    HEART_RATE("HEART RATE", "bpm", 0.075),
    CADENCE("CADENCE", "rpm", 0.05),
    NONE("OPEN", "", 0.0),
}

enum class TargetStatus { PUSH, IN_RANGE, EASE, NO_DATA }

/** One reading of the Karoo workout streams plus the rider's live output for the target kind. */
data class WorkoutSample(
    val stepIndex: Int?,
    val stepCount: Int,
    /** Null or negative for open-ended steps (lap button / distance). */
    val stepRemainingMs: Long?,
    val totalRemainingMs: Long?,
    val kind: TargetKind,
    val targetValue: Double?,
    val targetMin: Double?,
    val targetMax: Double?,
    val output: Double?,
)

data class WorkoutState(
    val stepNumber: Int,
    val stepCount: Int,
    val kind: TargetKind,
    val low: Int?,
    val high: Int?,
    val output: Int?,
    val status: TargetStatus,
    val stepRemainingS: Int?,
    val totalRemainingS: Int?,
    val stepElapsedS: Int,
    val stepAverage: Int?,
    val inTargetPercent: Int?,
    /** 0..1 through the current step, null for open steps. */
    val progress: Float?,
) {
    val isLastStep: Boolean get() = stepNumber >= stepCount
}

sealed class WorkoutEvent {
    data class Countdown(val secondsLeft: Int, val nextStepNumber: Int, val stepCount: Int) : WorkoutEvent()
    data class StepStarted(val state: WorkoutState) : WorkoutEvent()
    data object Finished : WorkoutEvent()
}

object TargetBand {
    /** Low/high bounds; a single target value gets the Karoo-style implied band for its kind. */
    fun bounds(kind: TargetKind, value: Double?, min: Double?, max: Double?): Pair<Int, Int>? {
        val lo = min?.takeIf { it > 0 }
        val hi = max?.takeIf { it > 0 }
        if (lo != null && hi != null && hi > lo) return lo.roundToInt() to hi.roundToInt()
        val v = value?.takeIf { it > 0 } ?: lo ?: hi ?: return null
        val band = v * kind.impliedBand
        return (v - band).roundToInt() to (v + band).roundToInt()
    }

    fun status(output: Int?, bounds: Pair<Int, Int>?): TargetStatus = when {
        output == null || bounds == null -> TargetStatus.NO_DATA
        output < bounds.first -> TargetStatus.PUSH
        output > bounds.second -> TargetStatus.EASE
        else -> TargetStatus.IN_RANGE
    }
}

/**
 * Turns workout samples into the Workout panel state and alert events. Call it several times a
 * second so no countdown second is skipped; step statistics are sampled at most once per second.
 * Karoo exposes only the running step, so the next step is known by number until it starts.
 */
class WorkoutTracker(
    private val countdownFrom: Int = 10,
    private val tickFrom: Int = 5,
) {
    private var lastStep: Int? = null
    private var lastAnnouncedSecond: Int? = null
    private var stepStartMs = 0L
    private var lastSampleMs: Long? = null
    private var stepSamples = 0
    private var stepSum = 0.0
    private var stepInTarget = 0
    private var active = false

    var state: WorkoutState? = null
        private set

    fun reset() {
        lastStep = null
        lastAnnouncedSecond = null
        lastSampleMs = null
        stepSamples = 0
        stepSum = 0.0
        stepInTarget = 0
        active = false
        state = null
    }

    fun update(sample: WorkoutSample?, nowMs: Long): List<WorkoutEvent> {
        val events = mutableListOf<WorkoutEvent>()
        val idx = sample?.stepIndex
        if (sample == null || sample.stepCount <= 0 || idx == null) {
            if (active) events += WorkoutEvent.Finished
            reset()
            return events
        }
        active = true
        val stepNumber = idx + 1
        val newStep = lastStep != idx
        if (newStep) {
            stepStartMs = nowMs
            lastSampleMs = null
            stepSamples = 0
            stepSum = 0.0
            stepInTarget = 0
            lastAnnouncedSecond = null
        }

        val bounds = TargetBand.bounds(sample.kind, sample.targetValue, sample.targetMin, sample.targetMax)
        val output = sample.output?.takeIf { it.isFinite() }?.roundToInt()
        val status = TargetBand.status(output, bounds)
        if (output != null && lastSampleMs.let { it == null || nowMs - it >= 1000 }) {
            lastSampleMs = nowMs
            stepSamples++
            stepSum += output
            if (status == TargetStatus.IN_RANGE) stepInTarget++
        }
        val elapsedS = ((nowMs - stepStartMs) / 1000).toInt()
        val remainingS = sample.stepRemainingMs?.takeIf { it >= 0 }?.let { ceil(it / 1000.0).toInt() }
        val progress = remainingS?.let { r ->
            val total = elapsedS + r
            if (total > 0) (elapsedS.toFloat() / total).coerceIn(0f, 1f) else null
        }
        val s = WorkoutState(
            stepNumber = stepNumber,
            stepCount = sample.stepCount,
            kind = sample.kind,
            low = bounds?.first,
            high = bounds?.second,
            output = output,
            status = status,
            stepRemainingS = remainingS,
            totalRemainingS = sample.totalRemainingMs?.takeIf { it >= 0 }?.let { ceil(it / 1000.0).toInt() },
            stepElapsedS = elapsedS,
            stepAverage = if (stepSamples > 0) (stepSum / stepSamples).roundToInt() else null,
            inTargetPercent = if (stepSamples > 0) (stepInTarget * 100.0 / stepSamples).roundToInt() else null,
            progress = progress,
        )
        state = s
        if (newStep && lastStep != null) events += WorkoutEvent.StepStarted(s)
        lastStep = idx

        if (remainingS != null && stepNumber < sample.stepCount && remainingS != lastAnnouncedSecond) {
            if (remainingS == countdownFrom || remainingS in 1..tickFrom) {
                events += WorkoutEvent.Countdown(remainingS, stepNumber + 1, sample.stepCount)
                lastAnnouncedSecond = remainingS
            }
        }
        return events
    }
}
