package io.github.mschache.karoopro.ui.field

import android.content.Context
import android.util.Log
import io.github.mschache.karoopro.AppGraph
import io.github.mschache.karoopro.EXTENSION_ID
import io.github.mschache.karoopro.data.Metric
import io.github.mschache.karoopro.data.Panel
import io.github.mschache.karoopro.karoo.ThrottledViewEmitter
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.UpdateGraphicConfig
import io.hammerhead.karooext.models.ViewConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The Panels field: KarooPro's panels without the big top, for the map page or any short slot. The
 * big number is the first slide; tap the left or right third to flip, the middle to switch the big
 * number. It keeps its own slide, apart from the full field.
 */
class PanelsFieldDataType(private val graph: AppGraph) : DataTypeImpl(EXTENSION_ID, TYPE_ID) {

    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        val out = ThrottledViewEmitter(emitter)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        out.onNext(UpdateGraphicConfig(showHeader = false))
        val density = context.resources.displayMetrics.density
        val (wPx, hPx) = config.viewSize
        Log.i(TAG, "Panels field view $wPx x $hPx px, grid ${config.gridSize}, preview=${config.preview}")
        val deck = graph.mapPanels

        scope.launch {
            if (config.preview) {
                // Page editor: a static example; taps must stay with the editor.
                val demo = DemoSource(graph.settings.value, graph.hub.profile.value)
                val model = FieldModelBuilder.compact(demo, Panel.NUMBER, listOf(Panel.NUMBER) + Panel.PANELS, density, false, FieldRenderer.isNight(context))
                out.updateView(FieldRenderer.panelsField(context, model, wPx, hPx))
                return@launch
            }
            val source = LiveSource(graph)
            weatherHold(scope)
            var keepAlive: Job? = null
            var keptTypes: Set<String> = emptySet()
            var last: CompactModel? = null
            while (isActive) {
                try {
                    val panel = deck.shown()
                    val types = FieldModelBuilder.requiredTypes(panel, graph.settings.value)
                    if (types != keptTypes) {
                        keepAlive?.cancel()
                        keepAlive = graph.hub.keepAlive(types)
                        keptTypes = types
                    }
                    if (isVisible()) {
                        val model = FieldModelBuilder.compact(source, panel, deck.visiblePanels(), density, interactive = true, systemDark = FieldRenderer.isNight(context))
                        if (model != last) {
                            out.updateView(FieldRenderer.panelsField(context, model, wPx, hPx))
                            last = model
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Panels render failed", e)
                }
                // Redraw on the next tick, or right away after a tap.
                withTimeoutOrNull(1_000) { deck.invalidations.first() }
            }
        }
        out.setCancellable { scope.cancel() }
    }

    /** Skip work while the page showing this field is not on screen. */
    private fun isVisible(): Boolean {
        val page = graph.hub.activePage.value ?: return true
        return page.elements.any { it.dataTypeId == dataTypeId }
    }

    /** Weather runs only while this field is placed and the Weather panel or a wind tile or card is on. */
    private fun weatherHold(scope: CoroutineScope) {
        scope.launch {
            var hold: Job? = null
            while (isActive) {
                val wanted = graph.settings.value.let { s ->
                    s.weatherEnabled && (
                        s.orderedPanels.any { it.panel == Panel.WEATHER && it.enabled } ||
                            (s.rideTiles + s.heroCards.orEmpty()).any { it in Metric.wind }
                        )
                }
                if (wanted && hold == null) hold = graph.weather.acquire()
                if (!wanted) {
                    hold?.cancel()
                    hold = null
                }
                delay(5_000)
            }
        }
    }

    companion object {
        const val TYPE_ID = "panels"
        private const val TAG = "KarooPRO"
    }
}
