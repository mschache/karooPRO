package io.github.mschache.karoopro.data

import io.github.mschache.karoopro.calc.PacePlan
import io.github.mschache.karoopro.calc.PaceSection
import io.github.mschache.karoopro.calc.Rig
import io.github.mschache.karoopro.calc.TargetBand
import io.github.mschache.karoopro.calc.TargetKind
import io.github.mschache.karoopro.calc.TargetStatus
import kotlinx.coroutines.flow.StateFlow

/** Where the rider stands against the pacing plan for the navigated route. */
data class PaceState(
    /** Target on the flat as % of FTP. */
    val intensity: Int,
    val target: Int,
    val low: Int,
    val high: Int,
    val output: Int?,
    val status: TargetStatus,
    /** The next change of target and the meters until it. */
    val next: PaceSection?,
    val toNext: Double?,
    /** Seconds ahead (+) or behind (-) the plan since pacing started; null until 100 m in. */
    val aheadS: Double?,
    val remainingS: Double,
    val etaMs: Long,
)

/**
 * A Power Guide-style plan for the navigated route (see [PacePlan]), rebuilt when the route or the
 * rider's numbers change, and the rider's place on it.
 */
class PaceTracker(
    private val hub: RideDataHub,
    private val routes: RouteTracker,
    private val settings: StateFlow<Settings>,
) {
    private class Ref(val routeKey: String, val along: Double, val movingS: Double)

    @Volatile private var cache: Pair<String, PacePlan>? = null
    @Volatile private var ref: Ref? = null

    fun state(nowMs: Long = System.currentTimeMillis()): PaceState? {
        val r = routes.current() ?: return null
        if (r.profile.isEmpty()) return null
        val s = settings.value
        val profile = hub.profile.value
        val ftp = profile.ftp.coerceAtLeast(1)
        val cp = s.criticalPower ?: ftp
        val key = "${r.key}:${s.paceIntensity}:$ftp:$cp:${profile.weightKg}:${s.bikeKg}:${s.cda}"
        val plan = cache?.takeIf { it.first == key }?.second
            ?: PacePlan.build(
                r.elevation,
                basePower = ftp * s.paceIntensity / 100.0,
                maxPower = cp.toDouble(),
                rig = Rig(massKg = profile.weightKg + s.bikeKg.toDouble(), cda = s.cda),
            ).also { cache = key to it }
        val along = routes.along(r)
        val section = plan.sectionAt(along) ?: return null
        val moving = hub.value(Metric.ELAPSED_TIME)?.div(1000)

        // Time against the plan counts from where pacing started; a new route or a jump back starts over.
        val start = ref
        if (moving != null && (start == null || start.routeKey != r.key || along < start.along - 200)) ref = Ref(r.key, along, moving)
        val aheadS = ref?.let { from ->
            if (moving == null || along - from.along < 100) null
            else (plan.timeAt(along) - plan.timeAt(from.along)) - (moving - from.movingS)
        }
        val bounds = TargetBand.bounds(TargetKind.POWER, section.power.toDouble(), null, null)!!
        val output = hub.value(Metric.POWER_3S)?.toInt()
        val next = plan.nextChange(along)
        val remaining = plan.totalSeconds - plan.timeAt(along)
        return PaceState(
            intensity = s.paceIntensity,
            target = section.power,
            low = bounds.first,
            high = bounds.second,
            output = output,
            status = TargetBand.status(output, bounds),
            next = next,
            toNext = next?.let { it.from - along },
            aheadS = aheadS,
            remainingS = remaining,
            etaMs = nowMs + (remaining * 1000).toLong(),
        )
    }
}
