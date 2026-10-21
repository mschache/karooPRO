package io.github.mschache.karoopro

import io.github.mschache.karoopro.numeric.HeadwindDataType
import io.github.mschache.karoopro.numeric.WPrimeDataType
import io.github.mschache.karoopro.ui.field.ProFieldDataType
import io.github.mschache.karoopro.ui.field.WindDataType
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.extension.KarooExtension

/** Entry point the Karoo binds to; started on install and at every boot. */
class KarooProExtension : KarooExtension(EXTENSION_ID, BuildConfig.VERSION_NAME) {

    override val types: List<DataTypeImpl> by lazy {
        val g = graph
        listOf(
            ProFieldDataType(g),
            WindDataType(g),
            WPrimeDataType(g),
            HeadwindDataType(g),
        )
    }

    override fun onBonusAction(actionId: String) {
        when (actionId) {
            ACTION_NEXT_PANEL -> graph.panels.step(1)
            ACTION_PREVIOUS_PANEL -> graph.panels.step(-1)
            ACTION_CYCLE_METRIC -> graph.cycleBigMetric()
        }
    }

    companion object {
        const val ACTION_NEXT_PANEL = "next-panel"
        const val ACTION_PREVIOUS_PANEL = "previous-panel"
        const val ACTION_CYCLE_METRIC = "cycle-metric"
    }
}
