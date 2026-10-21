package io.github.mschache.karoopro.data

import kotlinx.serialization.Serializable

enum class Panel(val title: String) {
    RIDE("RIDE"),
    EFFORT("EFFORT"),
    WEATHER("WEATHER"),
    LAP("LAP"),
    ANALYSIS("ANALYSIS"),
    WORKOUT("WORKOUT"),
    BIKE("BIKE"),
}

/** The value shown as the big number; tap the hero to cycle. */
enum class BigMetric(val label: String) { POWER("POWER"), SPEED("SPEED"), HR("HR") }

/** How the zone color is applied to the hero. */
enum class ColorMode(val label: String) {
    TINT("Tint the page"),
    FILL("Fill the top"),
    NUMBER("Color the number only"),
}

enum class ThemeMode(val label: String) { AUTO("Follow Karoo"), DARK("Dark"), LIGHT("Light") }

/** How the next workout step is announced inside the field. */
enum class AlertStyle(val label: String) { WINDOW("Window over the panel"), FULL("Full field"), OFF("Off") }

@Serializable
data class PanelSetting(val panel: Panel, val enabled: Boolean = true)

@Serializable
data class Settings(
    val panels: List<PanelSetting> = Panel.entries.map { PanelSetting(it) },
    val autoOpenWorkout: Boolean = true,
    val autoOpenBike: Boolean = true,
    val bigMetric: BigMetric = BigMetric.POWER,
    val colorMode: ColorMode = ColorMode.TINT,
    val theme: ThemeMode = ThemeMode.AUTO,
    val rideTiles: List<Metric> = listOf(Metric.ELAPSED_TIME, Metric.DISTANCE, Metric.CADENCE, Metric.CALORIES),
    val lapFields: List<LapMetric> = listOf(LapMetric.TIME, LapMetric.POWER, LapMetric.SPEED, LapMetric.HR),
    /** Critical power for W' balance in watts; null uses the FTP from the Karoo profile. */
    val criticalPower: Int? = null,
    val wPrimeJoules: Int = 20_000,
    val alertStyle: AlertStyle = AlertStyle.WINDOW,
    /** Also show a Karoo pop-up so the next step is announced on pages without KarooPRO. */
    val systemAlert: Boolean = true,
    val alertBeep: Boolean = true,
    val countdownBeeps: Boolean = true,
    val weatherEnabled: Boolean = true,
) {
    /** Panel order with any panels added in a newer version appended. */
    val orderedPanels: List<PanelSetting>
        get() = panels.distinctBy { it.panel } + Panel.entries.filter { p -> panels.none { it.panel == p } }.map { PanelSetting(it) }

    fun normalized(): Settings = copy(
        panels = orderedPanels,
        rideTiles = (rideTiles + Settings().rideTiles).take(4),
        lapFields = (lapFields + Settings().lapFields).take(4),
        wPrimeJoules = wPrimeJoules.coerceIn(1_000, 100_000),
        criticalPower = criticalPower?.takeIf { it in 50..1000 },
    )
}
