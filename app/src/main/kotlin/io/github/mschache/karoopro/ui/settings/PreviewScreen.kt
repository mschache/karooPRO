package io.github.mschache.karoopro.ui.settings

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.mschache.karoopro.graph
import io.github.mschache.karoopro.ui.field.DemoSource
import io.github.mschache.karoopro.ui.field.FieldModelBuilder
import io.github.mschache.karoopro.ui.field.FieldRenderer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withTimeoutOrNull

/** Options for the demo frame; also settable from adb intent extras for screenshots. */
data class PreviewOptions(val noPowerMeter: Boolean = false, val overlay: Int = 0, val map: Boolean = false)

/**
 * The real Pangu Dash field, rendered from demo data into this screen. Taps on the field run the same
 * actions as on a ride page (flip panels, switch the big number). "Dash Panels" shows the Panels field in a
 * map-page-sized slot instead.
 */
@Composable
fun PreviewScreen(initial: PreviewOptions = PreviewOptions()) {
    val context = LocalContext.current
    val graph = context.graph
    var options by remember { mutableStateOf(initial) }
    var size by remember { mutableStateOf(0 to 0) }
    val host = remember { FieldHost(context) { w, h -> size = w to h } }
    val currentOptions by rememberUpdatedState(options)
    val currentSize by rememberUpdatedState(size)

    DisposableEffect(Unit) {
        graph.panels.demoMode = true
        graph.mapPanels.demoMode = true
        onDispose {
            graph.panels.demoMode = false
            graph.mapPanels.demoMode = false
        }
    }
    // One loop: each frame finishes before the next starts (a Glance render can take over a second).
    LaunchedEffect(Unit) {
        var t = 0L
        while (true) {
            val (w, h) = currentSize
            if (w > 0 && h > 0) {
                val density = context.resources.displayMetrics.density
                val o = currentOptions
                // The Coach panel shows Pangu Coach's real message once it's set up.
                val coach = graph.coach.state.value?.takeIf { it.configured }
                val source = DemoSource(
                    graph.settings.value, graph.hub.profile.value, t = t, noPowerMeter = o.noPowerMeter, overlayMode = o.overlay,
                    liveCoach = coach, nowMs = if (coach != null) System.currentTimeMillis() else 0L,
                )
                if (o.map) {
                    // About what a map page hands a full-width field.
                    val mapH = (w * MAP_SLOT_RATIO).toInt()
                    val model = FieldModelBuilder.compact(
                        source, graph.mapPanels.shown(), graph.mapPanels.visiblePanels(), density,
                        interactive = true, systemDark = FieldRenderer.isNight(context),
                    )
                    host.show(FieldRenderer.panelsField(context, model, w, mapH), mapH)
                } else {
                    val model = FieldModelBuilder.build(
                        source, graph.panels.shown(), graph.panels.visiblePanels(), w / density, h / density, density,
                        interactive = true, systemDark = FieldRenderer.isNight(context),
                    )
                    val started = System.currentTimeMillis()
                    val views = FieldRenderer.field(context, model, w, h)
                    val composed = System.currentTimeMillis()
                    host.show(views)
                    android.util.Log.d(
                        "Pangu Dash",
                        "preview frame ${model.panel}: paint ${composed - started} ms, apply ${System.currentTimeMillis() - composed} ms",
                    )
                }
            }
            withTimeoutOrNull(1_000) { merge(graph.panels.invalidations, graph.mapPanels.invalidations).first() }
            t++
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Which field to show: the full page, or the panels as the map page has them. No PM and the alerts stay adb extras.
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            FieldTab("Dash", selected = !options.map, Modifier.weight(1f)) { options = options.copy(map = false) }
            FieldTab("Dash Panels", selected = options.map, Modifier.weight(1f)) { options = options.copy(map = true) }
        }
        AndroidView(factory = { host }, modifier = Modifier.fillMaxSize())
    }
}

/**
 * One of the two fields, in the mono face: the shown one in paper over a paper rule. Not orange: right above the
 * field's zone colors, nothing else is orange.
 */
@Composable
private fun FieldTab(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier.clickable(onClick = onClick).padding(top = 10.dp, bottom = 6.dp)) {
        MonoLabel(label, Modifier.padding(horizontal = 2.dp), color = if (selected) Horizon.Paper else Horizon.Second, size = 12.sp)
        Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(2.dp).background(if (selected) Horizon.Paper else Horizon.Line))
    }
}

/** Hosts applied RemoteViews and reports its size so the field can be laid out for it. */
class FieldHost(context: Context, private val onSize: (Int, Int) -> Unit) : FrameLayout(context) {
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        onSize(w, h)
    }

    /** Shows [views] filling the host, or [heightPx] tall at the top (the Panels field's slot). */
    fun show(views: android.widget.RemoteViews, heightPx: Int = LayoutParams.MATCH_PARENT) {
        val child = views.apply(context, this)
        removeAllViews()
        addView(child, LayoutParams(LayoutParams.MATCH_PARENT, heightPx))
    }
}

/** Height of the preview's map-page slot relative to its width. */
private const val MAP_SLOT_RATIO = 0.42f

