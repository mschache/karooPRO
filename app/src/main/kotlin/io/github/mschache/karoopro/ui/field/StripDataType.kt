package io.github.mschache.karoopro.ui.field

import android.content.Context
import android.util.Log
import io.github.mschache.karoopro.AppGraph
import io.github.mschache.karoopro.EXTENSION_ID
import io.github.mschache.karoopro.data.Metric
import io.github.mschache.karoopro.data.SegmentMonitor
import io.github.mschache.karoopro.karoo.ThrottledViewEmitter
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.UpdateGraphicConfig
import io.hammerhead.karooext.models.ViewConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Dash Strip: a slim row of Pangu Dash values for map and data pages, the ones the Karoo doesn't have
 * (W', stamina, wind, carbs), with the segment gap, workout target and distance to the top swapped
 * in while they matter.
 */
class StripDataType(private val graph: AppGraph) : DataTypeImpl(EXTENSION_ID, TYPE_ID) {

    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        val out = ThrottledViewEmitter(emitter)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        out.onNext(UpdateGraphicConfig(showHeader = false))
        val (wPx, hPx) = config.viewSize
        scope.launch {
            val source: RideSource = if (config.preview) DemoSource(graph.settings.value, graph.hub.profile.value) else LiveSource(graph)
            val metrics = graph.settings.value.stripMetrics
            val weather = if (!config.preview && metrics.any { it in Metric.wind }) graph.weather.acquire() else null
            val keep: Job? = if (config.preview) null else graph.hub.keepAlive(requiredTypes(metrics))
            try {
                var last: StripModel? = null
                while (isActive) {
                    try {
                        val model = FieldModelBuilder.strip(source, FieldRenderer.isNight(context))
                        if (model != last) {
                            out.updateView(FieldRenderer.image(context, FieldRenderer.painter(context).paintStrip(model, wPx, hPx)))
                            last = model
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e("KarooPRO", "Strip render failed", e)
                    }
                    if (config.preview) break
                    delay(1_000)
                }
            } finally {
                weather?.cancel()
                keep?.cancel()
            }
        }
        out.setCancellable { scope.cancel() }
    }

    private fun requiredTypes(metrics: List<Metric>): Set<String> = buildSet {
        metrics.mapNotNullTo(this) { it.karoo?.typeId }
        add(DataType.Type.SMOOTHED_3S_AVERAGE_POWER)
        add(DataType.Type.DISTANCE_TO_DESTINATION)
        addAll(SegmentMonitor.TYPES)
    }

    companion object {
        const val TYPE_ID = "strip"
    }
}
