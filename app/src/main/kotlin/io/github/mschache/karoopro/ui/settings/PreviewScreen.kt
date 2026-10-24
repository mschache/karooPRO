package io.github.mschache.karoopro.ui.settings

import android.content.Context
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.mschache.karoopro.graph
import io.github.mschache.karoopro.ui.field.DemoSource
import io.github.mschache.karoopro.ui.field.FieldModelBuilder
import io.github.mschache.karoopro.ui.field.FieldRenderer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** Options for the demo frame; also settable from adb intent extras for screenshots. */
data class PreviewOptions(val noPowerMeter: Boolean = false, val overlay: Int = 0)

/**
 * The real KarooPro field, rendered from demo data into this screen. Taps on the field run the same
 * actions as on a ride page (flip panels, switch the big number).
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
        onDispose { graph.panels.demoMode = false }
    }
    // One loop: each frame finishes before the next starts (a Glance render can take over a second).
    LaunchedEffect(Unit) {
        var t = 0L
        while (true) {
            val (w, h) = currentSize
            if (w > 0 && h > 0) {
                val density = context.resources.displayMetrics.density
                val o = currentOptions
                // The Coach panel shows KarooCoach's real message once it's set up.
                val coach = graph.coach.state.value?.takeIf { it.configured }
                val source = DemoSource(
                    graph.settings.value, graph.hub.profile.value, t = t, noPowerMeter = o.noPowerMeter, overlayMode = o.overlay,
                    liveCoach = coach, nowMs = if (coach != null) System.currentTimeMillis() else 0L,
                )
                val model = FieldModelBuilder.build(
                    source, graph.panels.shown(), graph.panels.visiblePanels(), w / density, h / density, density,
                    interactive = true, systemDark = FieldRenderer.isNight(context),
                )
                val started = System.currentTimeMillis()
                val views = FieldRenderer.field(context, model, w, h)
                val composed = System.currentTimeMillis()
                host.show(views)
                android.util.Log.d(
                    "KarooPro",
                    "preview frame ${model.panel}: paint ${composed - started} ms, apply ${System.currentTimeMillis() - composed} ms",
                )
            }
            withTimeoutOrNull(1_000) { graph.panels.invalidations.first() }
            t++
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            FilterChip(selected = options.noPowerMeter, onClick = { options = options.copy(noPowerMeter = !options.noPowerMeter) }, label = { Text("No PM") })
            FilterChip(selected = options.overlay == 1, onClick = { options = options.copy(overlay = if (options.overlay == 1) 0 else 1) }, label = { Text("Next") }, modifier = Modifier.padding(start = 4.dp))
            FilterChip(selected = options.overlay == 2, onClick = { options = options.copy(overlay = if (options.overlay == 2) 0 else 2) }, label = { Text("Step") }, modifier = Modifier.padding(start = 4.dp))
        }
        AndroidView(factory = { host }, modifier = Modifier.fillMaxSize())
    }
}

/** Hosts applied RemoteViews and reports its size so the field can be laid out for it. */
class FieldHost(context: Context, private val onSize: (Int, Int) -> Unit) : FrameLayout(context) {
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        onSize(w, h)
    }

    fun show(views: android.widget.RemoteViews) {
        val child = views.apply(context, this)
        removeAllViews()
        addView(child, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }
}

