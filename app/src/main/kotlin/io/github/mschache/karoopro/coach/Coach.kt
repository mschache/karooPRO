package io.github.mschache.karoopro.coach

import io.github.mschache.karoopro.calc.Fmt
import io.github.mschache.karoopro.calc.StepSummary
import io.github.mschache.karoopro.calc.TargetKind
import io.github.mschache.karoopro.calc.WindComponents
import io.github.mschache.karoopro.calc.WorkoutState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.abs
import kotlin.math.round

/** A message from KarooCoach; [receivedAt] is when the Karoo got it (epoch ms). */
data class CoachMessage(val id: String, val title: String, val text: String, val receivedAt: Long)

/**
 * One upcoming step of a planned workout. [index] is the Karoo's 0-based step; [low]/[high] are
 * watts (equal for a steady step, from → to for a [ramp]), null for a step without a power target
 * or when the relay doesn't know the FTP; [lowPct]/[highPct] are the same targets in % of FTP.
 */
@Serializable
data class PlanStep(
    val index: Int,
    val seconds: Int,
    val low: Int? = null,
    val high: Int? = null,
    val ramp: Boolean = false,
    val label: String? = null,
    val lowPct: Double? = null,
    val highPct: Double? = null,
) {
    /**
     * Watts from the % of FTP targets with the Karoo's [ftp] when the relay sent none. Halves round
     * to even, as on the Karoo's own workout screen: 105 % of 250 W is 262 W there and here.
     */
    fun inWatts(ftp: Int): PlanStep {
        if (low != null || lowPct == null || ftp <= 0) return this
        fun watts(pct: Double) = round(pct * ftp / 100).toInt()
        return copy(low = watts(lowPct), high = watts(highPct ?: lowPct))
    }
}

/**
 * The plan KarooCoach's relay matched to the running workout (from intervals.icu or a workout
 * pushed through it): [count] steps, matched while step [index] ran, with up to three [steps] after it.
 */
@Serializable
data class CoachPlan(
    val count: Int,
    val index: Int,
    val name: String? = null,
    val steps: List<PlanStep> = emptyList(),
) {
    /**
     * The step after the running step [stepIndex], when this plan has the running workout's
     * [stepCount] and is at most [MAX_AGE_MS] old ([ageMs]); null otherwise, so a stale or
     * mismatched plan never shows.
     */
    fun nextFor(stepIndex: Int, stepCount: Int, ageMs: Long): PlanStep? {
        if (count != stepCount || ageMs > MAX_AGE_MS) return null
        return steps.firstOrNull { it.index == stepIndex + 1 }
    }

    companion object {
        /** Check-ins run every 10–20 s during a workout, so a plan older than this has lost its match. */
        const val MAX_AGE_MS = 120_000L
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(text: String?): CoachPlan? =
            text?.takeIf { it.isNotBlank() }?.let { runCatching { json.decodeFromString(serializer(), it) }.getOrNull() }
    }
}

object PlanText {
    /** "280", "280–300" or "200→260" in watts; null for a step without a power target. */
    fun target(step: PlanStep): String? {
        val low = step.low ?: return null
        val high = step.high ?: low
        return when {
            step.ramp && high != low -> "$low→$high"
            high != low -> "$low–$high"
            else -> "$low"
        }
    }

    /** "4:00 @ 280 W", or "4:00 OPEN" without a power target. */
    fun summary(step: PlanStep): String =
        Fmt.seconds(step.seconds.toLong()) + (target(step)?.let { " @ $it W" } ?: " OPEN")
}

/** KarooCoach's shared state, read from its content provider (see [CoachContract]). */
data class CoachInfo(
    val configured: Boolean,
    /** Check-ins are running: set up, riding, and the ride profile shows the coach. */
    val active: Boolean,
    val riding: Boolean,
    val lastCheckInAt: Long?,
    val lastError: String?,
    val message: CoachMessage?,
    val acknowledged: Boolean,
    /** The workout plan from the last check-in, received at [planAt] (epoch ms). */
    val plan: CoachPlan? = null,
    val planAt: Long? = null,
) {
    val unread: Boolean get() = message != null && !acknowledged

    /** The planned step after the running step [stepIndex] (0-based), when the plan matches and is fresh. */
    fun nextStep(stepIndex: Int, stepCount: Int, now: Long): PlanStep? {
        val at = planAt ?: return null
        return plan?.nextFor(stepIndex, stepCount, now - at)
    }

    companion object {
        /** Parses one provider row given as column name to value; null when the row isn't usable. */
        fun from(row: Map<String, Any?>): CoachInfo? {
            if ((row.long(CoachContract.COL_VERSION) ?: return null) > CoachContract.VERSION) return null
            val id = row[CoachContract.COL_MESSAGE_ID] as? String
            val text = row[CoachContract.COL_MESSAGE_TEXT] as? String
            return CoachInfo(
                configured = row.flag(CoachContract.COL_CONFIGURED),
                active = row.flag(CoachContract.COL_ACTIVE),
                riding = row.flag(CoachContract.COL_RIDING),
                lastCheckInAt = row.long(CoachContract.COL_LAST_CHECK_IN_AT),
                lastError = row[CoachContract.COL_LAST_ERROR] as? String,
                message = if (id != null && text != null) {
                    CoachMessage(
                        id = id,
                        title = (row[CoachContract.COL_MESSAGE_TITLE] as? String)?.takeIf { it.isNotBlank() } ?: "Coach",
                        text = text,
                        receivedAt = row.long(CoachContract.COL_MESSAGE_AT) ?: 0L,
                    )
                } else {
                    null
                },
                acknowledged = row.flag(CoachContract.COL_ACKNOWLEDGED),
                plan = CoachPlan.parse(row[CoachContract.COL_PLAN_JSON] as? String),
                planAt = row.long(CoachContract.COL_PLAN_AT),
            )
        }

        private fun Map<String, Any?>.long(key: String): Long? = (this[key] as? Number)?.toLong()
        private fun Map<String, Any?>.flag(key: String): Boolean = long(key) == 1L
    }
}

enum class CoachStatus(val label: String, val warning: Boolean) {
    NOT_SET_UP("NOT SET UP", true),
    WAITING("WAITING FOR A RIDE", false),
    OFF("OFF FOR THIS PROFILE", true),
    CONNECTING("CONNECTING", false),
    LIVE("LIVE", false),
    OFFLINE("OFFLINE", true),
    ;

    companion object {
        /** Same rules as the KarooCoach field, so both show the same status. */
        fun of(info: CoachInfo, now: Long): CoachStatus = when {
            !info.configured -> NOT_SET_UP
            !info.riding -> WAITING
            !info.active -> OFF
            info.lastError != null && (info.lastCheckInAt == null || now - info.lastCheckInAt > OFFLINE_AFTER_MS) -> OFFLINE
            info.lastCheckInAt != null -> LIVE
            else -> CONNECTING
        }

        private const val OFFLINE_AFTER_MS = 90_000L
    }
}

object CoachText {
    fun ago(ms: Long): String = when {
        ms < 60_000 -> "just now"
        ms < 3_600_000 -> "${ms / 60_000} min ago"
        else -> "${ms / 3_600_000} h ago"
    }
}

/** Decides when a coach message opens the Coach panel: once per message, while it's unread and fresh. */
class CoachInbox {
    private var seenId: String? = null

    fun isNew(info: CoachInfo?, now: Long): Boolean {
        val m = info?.message ?: return false
        if (m.id == seenId) return false
        seenId = m.id
        return !info.acknowledged && now - m.receivedAt < FRESH_MS
    }

    private companion object {
        /** Older unread messages (e.g. after KarooPro restarts) stay on the panel without opening it. */
        const val FRESH_MS = 5 * 60_000L
    }
}

/**
 * What KarooPro tells KarooCoach with [CoachContract.METHOD_EFFORT]: values only KarooPro computes,
 * as doubles in SI units (W, J, m/s, s; workout values in the step's target unit). Unknown values
 * are left out; the W′ model's CP and W′ come only with a W′ balance.
 */
object CoachEffort {
    fun values(
        wPrimeJ: Double?,
        wPrimeMaxJ: Double,
        cpW: Double,
        /** Best average power in W by duration in seconds. */
        best: Map<Int, Double?>,
        wind: WindComponents?,
        step: WorkoutState?,
        lastStep: StepSummary?,
    ): Map<String, Double> = buildMap {
        fun putKnown(key: String, v: Number?) {
            v?.toDouble()?.takeIf { it.isFinite() }?.let { put(key, it) }
        }
        if (wPrimeJ != null && wPrimeJ.isFinite()) {
            putKnown(CoachContract.W_PRIME_J, wPrimeJ)
            putKnown(CoachContract.W_PRIME_MAX_J, wPrimeMaxJ)
            putKnown(CoachContract.CP_W, cpW)
        }
        for ((seconds, key) in CoachContract.BEST) putKnown(key, best[seconds])
        if (wind != null) {
            putKnown(CoachContract.HEAD_MS, wind.head)
            putKnown(CoachContract.CROSS_MS, abs(wind.cross))
        }
        if (step != null) {
            putKnown(CoachContract.STEP_AVG, step.stepAverage)
            putKnown(CoachContract.STEP_IN_TARGET, step.inTargetPercent)
        }
        if (lastStep != null) {
            putKnown(CoachContract.LAST_STEP_INDEX, lastStep.index)
            putKnown(CoachContract.LAST_STEP_KIND, kindCode(lastStep.kind))
            putKnown(CoachContract.LAST_STEP_LOW, lastStep.low)
            putKnown(CoachContract.LAST_STEP_HIGH, lastStep.high)
            putKnown(CoachContract.LAST_STEP_SECONDS, lastStep.seconds)
            putKnown(CoachContract.LAST_STEP_AVG, lastStep.average)
            putKnown(CoachContract.LAST_STEP_IN_TARGET, lastStep.inTargetPercent)
        }
    }

    fun kindCode(kind: TargetKind): Int = when (kind) {
        TargetKind.POWER -> 1
        TargetKind.HEART_RATE -> 2
        TargetKind.CADENCE -> 3
        TargetKind.NONE -> 0
    }
}

/** KarooCoach's side of the link; mirrors CoachContract in the karoo-mcp extension. */
object CoachContract {
    const val PACKAGE = "io.github.mschache.karoocoach"
    const val STATE_URI = "content://io.github.mschache.karoocoach.state/state"
    const val VERSION = 1

    const val COL_VERSION = "version"
    const val COL_CONFIGURED = "configured"
    const val COL_ACTIVE = "active"
    const val COL_RIDING = "riding"
    const val COL_LAST_CHECK_IN_AT = "last_check_in_at"
    const val COL_LAST_ERROR = "last_error"
    const val COL_MESSAGE_ID = "message_id"
    const val COL_MESSAGE_TITLE = "message_title"
    const val COL_MESSAGE_TEXT = "message_text"
    const val COL_MESSAGE_AT = "message_at"
    const val COL_ACKNOWLEDGED = "acknowledged"
    /** The relay's plan for the running workout as JSON ([CoachPlan]), and when it arrived (epoch ms). */
    const val COL_PLAN_JSON = "plan_json"
    const val COL_PLAN_AT = "plan_at"

    const val METHOD_ACK = "ack"
    const val METHOD_HOST = "host"
    const val EXTRA_SHOWS = "shows"
    const val EXTRA_OPENS = "opens"

    /** Sent about every 5 s while [COL_ACTIVE]; see [CoachEffort]. */
    const val METHOD_EFFORT = "effort"
    const val W_PRIME_J = "w_prime_j"
    const val W_PRIME_MAX_J = "w_prime_max_j"
    const val CP_W = "cp_w"
    const val BEST_5 = "best_5"
    const val BEST_60 = "best_60"
    const val BEST_300 = "best_300"
    const val BEST_1200 = "best_1200"
    /** Wind along the direction of travel in m/s: + headwind, − tailwind. */
    const val HEAD_MS = "head_ms"
    /** Size of the crosswind in m/s. */
    const val CROSS_MS = "cross_ms"
    /** Running workout step: average and % of time in target, in the step's target unit. */
    const val STEP_AVG = "step_avg"
    const val STEP_IN_TARGET = "step_in_target"
    /** The step that just ended; index is the Karoo's 0-based step, kind 1 power, 2 heart rate, 3 cadence, 0 none. */
    const val LAST_STEP_INDEX = "last_step_index"
    const val LAST_STEP_KIND = "last_step_kind"
    const val LAST_STEP_LOW = "last_step_low"
    const val LAST_STEP_HIGH = "last_step_high"
    const val LAST_STEP_SECONDS = "last_step_seconds"
    const val LAST_STEP_AVG = "last_step_avg"
    const val LAST_STEP_IN_TARGET = "last_step_in_target"

    /** Best-effort keys by duration in seconds. */
    val BEST = mapOf(5 to BEST_5, 60 to BEST_60, 300 to BEST_300, 1200 to BEST_1200)
}
