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

data class WorkoutContent(val state: WorkoutState?, val unit: String) : PanelContent

data class BikeContent(
    val gearing: Gearing?,
    val gears: String,
    val teeth: String,
    val batteries: List<Pair<String, BatteryStatus?>>,
    val balance: Stat,
    val balanceLeft: Float?,
    val stats: List<Stat>,
) : PanelContent

sealed interface OverlayModel {
    val full: Boolean

    data class NextStep(val nextStep: Int, val stepCount: Int, val secondsLeft: Int, override val full: Boolean) : OverlayModel
    data class NewTarget(val title: String, val target: String, val unit: String, override val full: Boolean) : OverlayModel
}
