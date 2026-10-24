package io.github.mschache.karoopro.ui.field

import io.github.mschache.karoopro.calc.Gearing
import io.github.mschache.karoopro.calc.Trend
import io.github.mschache.karoopro.calc.WindKind
import io.github.mschache.karoopro.calc.WorkoutState
import io.github.mschache.karoopro.data.BigMetric
import io.github.mschache.karoopro.data.Panel
import io.hammerhead.karooext.models.BatteryStatus

/** Colors for one frame; all ARGB ints. */
data class Palette(
    val dark: Boolean,
    val background: Int,
    val card: Int,
    val text: Int,
    val dim: Int,
    val faint: Int,
    val accent: Int,
    val good: Int,
    val warn: Int,
    val bad: Int,
    val push: Int,
) {
    companion object {
        val DARK = Palette(
            dark = true,
            background = 0xFF000000.toInt(),
            card = 0xFF17191C.toInt(),
            text = 0xFFFFFFFF.toInt(),
            dim = 0xFFA0A6AD.toInt(),
            faint = 0xFF3A3F45.toInt(),
            accent = 0xFF2E9BFF.toInt(),
            good = 0xFF00C46A.toInt(),
            warn = 0xFFFFC400.toInt(),
            bad = 0xFFFF3B30.toInt(),
            push = 0xFF2E9BFF.toInt(),
        )
        val LIGHT = Palette(
            dark = false,
            background = 0xFFFFFFFF.toInt(),
            card = 0xFFF0F1F3.toInt(),
            text = 0xFF000000.toInt(),
            dim = 0xFF5B6168.toInt(),
            faint = 0xFFD5D8DC.toInt(),
            accent = 0xFF0A6FD6.toInt(),
            good = 0xFF00995A.toInt(),
            warn = 0xFFB88A00.toInt(),
            bad = 0xFFD7261E.toInt(),
            push = 0xFF0A6FD6.toInt(),
        )
    }
}

data class FieldModel(
    val palette: Palette,
    val widthDp: Float,
    val heightDp: Float,
    val density: Float,
    val hero: HeroModel,
    val panels: List<Panel>,
    val panel: Panel,
    val content: PanelContent,
    val overlay: OverlayModel?,
    val interactive: Boolean,
    /** Background of the whole field; the zone tint when the color mode tints the page. */
    val pageBackground: Int,
)

data class HeroModel(
    val big: BigMetric,
    val options: List<BigMetric>,
    val value: String,
    val unit: String,
    val zone: Int?,
    val zoneColor: Int?,
    val zoneColors: List<Int>,
    /** Background of the hero block and the color of the big number on it. */
    val background: Int,
    val numberColor: Int,
    val textColor: Int,
    val dimColor: Int,
    val stats: List<Stat>,
    val cards: List<CardModel>,
)

data class Stat(val label: String, val value: String, val unit: String = "", val accent: Int? = null)

data class CardModel(val label: String, val value: String, val unit: String, val avg: String?, val accent: Int?)

sealed interface PanelContent

data class TileModel(val label: String, val value: String, val unit: String, val graph: List<Float?>?, val graphColor: Int)

data class RideContent(val tiles: List<TileModel>) : PanelContent

data class EffortContent(
    /** Average power per 20 s for the last 10 minutes, oldest first, with its zone color. */
    val bars: List<Pair<Float?, Int>>,
    val barMax: Float,
    val wPrime: Stat,
    val wPrimeFraction: Float?,
    val stats: List<Stat>,
    /** (meters relative to the rider, elevation) pairs; negative = behind. */
    val terrain: List<Pair<Float, Float>>,
    val terrainLabel: String,
) : PanelContent

/** One slice of a climb's profile: [from]/[to] 0..1 along the climb, heights 0..1 from its foot to its top, grade in %. */
data class ClimbBar(val from: Float, val to: Float, val fromHeight: Float, val toHeight: Float, val grade: Float)

data class ClimbContent(
    /** "CLIMB 2/5", or "CLIMB 2/5 IN 1.20 KM" before it starts. */
    val title: String,
    /** "W′ EMPTY IN 3:20" or "W′ OK"; null without a power meter. */
    val effort: String?,
    /** W′ runs out before the top at this power. */
    val effortWarning: Boolean,
    /** Two large values: to the top on the climb, the climb's length and gain before it. */
    val big: List<Stat>,
    val stats: List<Stat>,
    val bars: List<ClimbBar>,
    /** The rider's place 0..1 along the climb; null before it starts. */
    val rider: Float?,
    /** Shown instead of everything else when there's no climb. */
    val message: String? = null,
) : PanelContent

data class SegmentContent(
    /** "SEGMENT", "SEGMENT IN 320 m" or "SEGMENT DONE". */
    val title: String,
    /** Large: the gap to your PR (your PR time before the start). */
    val main: Stat,
    /** True when ahead (green), false when behind (red), null when it isn't a gap. */
    val mainAhead: Boolean?,
    /** The gap to the KOM (the KOM time before the start). */
    val second: Stat,
    val secondAhead: Boolean?,
    /** 0..1 through the segment, when known. */
    val progress: Float?,
    val stats: List<Stat>,
    /** Shown instead of everything else when there's no segment. */
    val message: String? = null,
) : PanelContent

/** One finished climb or segment: [good] colors the main value (ahead of the PR), null leaves it plain. */
data class ResultRow(val title: String, val main: String, val detail: String, val good: Boolean?)

data class ResultsContent(val rows: List<ResultRow>) : PanelContent

data class FuelContent(
    val title: String,
    val eaten: Stat,
    val burned: Stat,
    /** Eaten minus burned, and whether intake keeps up with the target so far. */
    val balance: Stat,
    val onTrack: Boolean,
    /** Eaten as a share of the target so far, 0..1+; null before the target starts. */
    val targetFraction: Float?,
    val eat: String,
    val eatDue: Boolean,
    val drink: String,
    val drinkDue: Boolean,
    val hint: String,
) : PanelContent

data class PaceContent(
    val title: String,
    val target: String,
    val low: Int?,
    val high: Int?,
    val output: Int?,
    val status: io.github.mschache.karoopro.calc.TargetStatus,
    val next: String,
    val stats: List<Stat>,
    val message: String? = null,
) : PanelContent

/** One value on the PRO Strip; [color] overrides the text color. */
data class StripCell(val label: String, val value: String, val unit: String, val color: Int? = null)

data class StripModel(val palette: Palette, val cells: List<StripCell>)

data class HourModel(val label: String, val speed: String, val relativeToDeg: Float?, val kind: WindKind?)

data class WeatherContent(
    val message: String?,
    val kind: WindKind?,
    val component: String,
    val unit: String,
    /** Direction the wind blows towards, relative to the direction of travel (0 = pushing you). */
    val relativeToDeg: Float?,
    val stats: List<Stat>,
    val hours: List<HourModel>,
) : PanelContent

data class LapFieldModel(val label: String, val value: String, val unit: String, val previous: String?, val trend: Trend)

data class LapContent(val title: String, val fields: List<LapFieldModel>) : PanelContent

data class ZoneBar(val label: String, val seconds: Long, val fraction: Float, val color: Int)

data class AnalysisContent(val best: List<Stat>, val zoneTitle: String, val zones: List<ZoneBar>, val totals: List<Stat>) : PanelContent

/** [next] is the planned next step, e.g. "4:00 @ 280 W", when Karoo Coach's relay knows the workout. */
data class WorkoutContent(val state: WorkoutState?, val unit: String, val next: String? = null) : PanelContent

data class BikeContent(
    val gearing: Gearing?,
    val gears: String,
    val teeth: String,
    val batteries: List<Pair<String, BatteryStatus?>>,
    val balance: Stat,
    val balanceLeft: Float?,
    val stats: List<Stat>,
) : PanelContent

data class CoachContent(
    val status: String,
    val warning: Boolean,
    /** How old the message is, e.g. "2 min ago"; empty without a message. */
    val age: String,
    /** The message title when it says more than "Coach". */
    val title: String?,
    val text: String,
    /** No message yet: [text] is a hint, drawn dim. */
    val placeholder: Boolean,
    val unread: Boolean,
    /** "TAP TO CONFIRM" while unread, "READ" after. */
    val hint: String,
) : PanelContent

sealed interface OverlayModel {
    val full: Boolean

    /** [target] (watts) and [duration] come from the plan, when known. */
    data class NextStep(
        val nextStep: Int,
        val stepCount: Int,
        val secondsLeft: Int,
        override val full: Boolean,
        val target: String? = null,
        val duration: String? = null,
    ) : OverlayModel
    data class NewTarget(val title: String, val target: String, val unit: String, override val full: Boolean) : OverlayModel
}
