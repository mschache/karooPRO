package io.github.mschache.karoopro

import io.github.mschache.karoopro.calc.FuelItem
import io.github.mschache.karoopro.numeric.Best20DataType
import io.github.mschache.karoopro.numeric.CrosswindDataType
import io.github.mschache.karoopro.numeric.HeadwindDataType
import io.github.mschache.karoopro.numeric.WPrimeDataType
import io.github.mschache.karoopro.numeric.WPrimeEmptyDataType
import io.github.mschache.karoopro.ui.field.PanelsFieldDataType
import io.github.mschache.karoopro.ui.field.ProFieldDataType
import io.github.mschache.karoopro.ui.field.StripDataType
import io.github.mschache.karoopro.ui.field.WindDataType
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.extension.KarooExtension
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.models.FitEffect
import io.hammerhead.karooext.models.MapEffect

/** Entry point the Karoo binds to; started on install and at every boot. */
class KarooProExtension : KarooExtension(EXTENSION_ID, BuildConfig.VERSION_NAME) {

    override val types: List<DataTypeImpl> by lazy {
        val g = graph
        listOf(
            ProFieldDataType(g),
            WindDataType(g),
            WPrimeDataType(g),
            HeadwindDataType(g),
            WPrimeEmptyDataType(g),
            CrosswindDataType(g),
            Best20DataType(g),
            StripDataType(g),
            PanelsFieldDataType(g),
        )
    }

    override fun startMap(emitter: Emitter<MapEffect>) {
        val job = graph.mapLayers.run(emitter)
        emitter.setCancellable { job.cancel() }
    }

    override fun startFit(emitter: Emitter<FitEffect>) {
        val job = graph.fit.run(emitter)
        emitter.setCancellable { job.cancel() }
    }

    override fun onBonusAction(actionId: String) {
        when (actionId) {
            ACTION_NEXT_PANEL -> graph.stepPanels(1)
            ACTION_PREVIOUS_PANEL -> graph.stepPanels(-1)
            ACTION_CYCLE_METRIC -> graph.cycleBigMetric()
            ACTION_LOG_GEL -> graph.logFuel(FuelItem.GEL)
            ACTION_LOG_BAR -> graph.logFuel(FuelItem.BAR)
            ACTION_LOG_BOTTLE -> graph.logFuel(FuelItem.BOTTLE)
        }
    }

    companion object {
        const val ACTION_NEXT_PANEL = "next-panel"
        const val ACTION_PREVIOUS_PANEL = "previous-panel"
        const val ACTION_CYCLE_METRIC = "cycle-metric"
        const val ACTION_LOG_GEL = "log-gel"
        const val ACTION_LOG_BAR = "log-bar"
        const val ACTION_LOG_BOTTLE = "log-bottle"
    }
}
