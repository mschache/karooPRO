package io.github.mschache.karoopro.ui.field

import android.content.Context
import android.util.Log
import io.github.mschache.karoopro.AppGraph
import io.github.mschache.karoopro.EXTENSION_ID
import io.github.mschache.karoopro.data.Panel
import io.github.mschache.karoopro.karoo.ThrottledViewEmitter
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.UpdateGraphicConfig
import io.hammerhead.karooext.models.ViewConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Standalone Wind field: a compass that turns with your heading plus the head/tail/cross value. */
class WindDataType(private val graph: AppGraph) : DataTypeImpl(EXTENSION_ID, "wind") {

    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        val out = ThrottledViewEmitter(emitter)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        out.onNext(UpdateGraphicConfig(showHeader = false))
        val density = context.resources.displayMetrics.density
        val (wPx, hPx) = config.viewSize
        scope.launch {
            val source: RideSource = if (config.preview) DemoSource(graph.settings.value, graph.hub.profile.value) else LiveSource(graph)
            val hold = if (config.preview) null else graph.weather.acquire()
            try {
                var last: WeatherContent? = null
                while (isActive) {
                    try {
                        val model = FieldModelBuilder.build(source, Panel.WEATHER, listOf(Panel.WEATHER), wPx / density, hPx / density, density, false, FieldRenderer.isNight(context))
                        val content = model.content as WeatherContent
                        if (content != last) {
                            out.updateView(FieldRenderer.image(context, FieldRenderer.painter(context).paintWind(model.palette, content, wPx, hPx)))
                            last = content
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e("KarooPRO", "Wind render failed", e)
                    }
                    if (config.preview) break
                    delay(1_000)
                }
            } finally {
                hold?.cancel()
            }
        }
        out.setCancellable { scope.cancel() }
    }
}
