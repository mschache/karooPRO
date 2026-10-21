package io.github.mschache.karoopro.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.mschache.karoopro.data.Panel
import io.github.mschache.karoopro.graph
import kotlinx.coroutines.launch

enum class Screen { HOME, PREVIEW, SETTINGS, ABOUT }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // adb: am start -n io.github.mschache.karoopro/.ui.settings.MainActivity --es screen PREVIEW --es panel BIKE
        val start = intent.getStringExtra("screen")?.let { runCatching { Screen.valueOf(it) }.getOrNull() } ?: Screen.HOME
        intent.getStringExtra("panel")?.let { name -> runCatching { Panel.valueOf(name) }.getOrNull()?.let { graph.panels.show(it) } }
        // Debug builds: adb ... --es weathertest "47.37,8.54" fetches Open-Meteo through the Karoo and logs it.
        if (io.github.mschache.karoopro.BuildConfig.DEBUG) {
            intent.getStringExtra("weathertest")?.split(",")?.mapNotNull { it.trim().toDoubleOrNull() }?.takeIf { it.size == 2 }?.let { (lat, lng) ->
                graph.scope.launch {
                    val started = System.currentTimeMillis()
                    val result = runCatching { graph.weather.fetch(lat, lng) }
                    android.util.Log.i("KarooPRO", "weathertest ${System.currentTimeMillis() - started} ms: $result")
                }
            }
        }
        // Debug builds: --ez alerttest true plays the "next step" beep and pop-up 6 s later (time to switch to the ride app).
        if (io.github.mschache.karoopro.BuildConfig.DEBUG && intent.getBooleanExtra("alerttest", false)) {
            graph.scope.launch {
                kotlinx.coroutines.delay(6_000)
                graph.alerts.beep(io.github.mschache.karoopro.alerts.Beep.NEXT_SOON)
                graph.alerts.popup("karoopro-test", "Next: step 3/8", "in 10 s", 8_000)
            }
        }
        // Debug builds: --ez exportshots true writes every panel, painted at ride-page size, to app files/shots.
        if (io.github.mschache.karoopro.BuildConfig.DEBUG && intent.getBooleanExtra("exportshots", false)) {
            graph.scope.launch {
                val dir = java.io.File(getExternalFilesDir(null), "shots").apply { mkdirs() }
                val density = resources.displayMetrics.density
                val (w, h) = 478 to 642
                val painter = io.github.mschache.karoopro.ui.field.FieldRenderer.painter(this@MainActivity)
                for ((i, panel) in Panel.entries.withIndex()) {
                    for (overlay in listOf(0, 1)) {
                        if (overlay == 1 && panel != Panel.WORKOUT) continue
                        val src = io.github.mschache.karoopro.ui.field.DemoSource(graph.settings.value, graph.hub.profile.value, t = 12L * i, overlayMode = overlay)
                        val model = io.github.mschache.karoopro.ui.field.FieldModelBuilder.build(
                            src, panel, Panel.entries, w / density, h / density, density, interactive = false, systemDark = true,
                        )
                        val name = panel.name.lowercase() + if (overlay == 1) "-alert" else ""
                        java.io.File(dir, "$name.png").outputStream().use { painter.paint(model, w, h).compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                    }
                }
                android.util.Log.i("KarooPRO", "exportshots written to $dir")
            }
        }
        val previewOptions = PreviewOptions(
            noPowerMeter = intent.getBooleanExtra("nopower", false),
            overlay = intent.getIntExtra("overlay", 0),
        )
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF2E9BFF))) {
                var screen by remember { mutableStateOf(start) }
                BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }
                Box(Modifier.fillMaxSize().background(Color.Black)) {
                    when (screen) {
                        Screen.HOME -> HomeScreen { screen = it }
                        Screen.PREVIEW -> PreviewScreen(previewOptions)
                        Screen.SETTINGS -> SettingsScreen()
                        Screen.ABOUT -> AboutScreen()
                    }
                }
            }
        }
    }
}
