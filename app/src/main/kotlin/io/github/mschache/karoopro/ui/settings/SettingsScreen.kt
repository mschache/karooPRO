package io.github.mschache.karoopro.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.mschache.karoopro.BuildConfig
import io.github.mschache.karoopro.data.AlertStyle
import io.github.mschache.karoopro.data.BigMetric
import io.github.mschache.karoopro.data.ColorMode
import io.github.mschache.karoopro.data.LapMetric
import io.github.mschache.karoopro.data.Metric
import io.github.mschache.karoopro.data.Settings
import io.github.mschache.karoopro.data.ThemeMode
import io.github.mschache.karoopro.graph
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(open: (Screen) -> Unit) {
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("KarooPRO", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Ride by effort", fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
        Text(
            "Add a ride page with one field and choose KarooPRO. Tap the top to switch power, speed and heart rate; tap the left or right of the panel to flip.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )
        Button(onClick = { open(Screen.PREVIEW) }, modifier = Modifier.fillMaxWidth()) { Text("Preview") }
        Button(onClick = { open(Screen.SETTINGS) }, modifier = Modifier.fillMaxWidth()) { Text("Settings") }
        OutlinedButton(onClick = { open(Screen.ABOUT) }, modifier = Modifier.fillMaxWidth()) { Text("About") }
    }
}

@Composable
fun AboutScreen() {
    Column(Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val c = MaterialTheme.colorScheme.onBackground
        Text("KarooPRO ${BuildConfig.VERSION_NAME}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = c)
        Text("Free and open source (Apache-2.0): github.com/mschache/karooPRO", color = c, fontSize = 13.sp)
        Text("Weather data by Open-Meteo.com (CC BY 4.0).", color = c, fontSize = 13.sp)
        Text("Zone colors follow the Karoo palette as sampled by the open-source BigNum extension.", color = c, fontSize = 13.sp)
        Text(
            "The Karoo only shares the running workout step, so the next step's target appears when it starts; the countdown before it is exact.",
            color = c.copy(alpha = 0.7f),
            fontSize = 12.sp,
        )
        Text("Not affiliated with Hammerhead or SRAM.", color = c.copy(alpha = 0.7f), fontSize = 12.sp)
    }
}

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val repo = context.graph.settingsRepository
    val settings by repo.settings.collectAsState()
    val scope = rememberCoroutineScope()
    val update: ((Settings) -> Settings) -> Unit = { f -> scope.launch { repo.update(f) } }
    var picker by remember { mutableStateOf<Picker?>(null) }

    picker?.let { p ->
        PickerScreen(p) { picker = null }
        return
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 6.dp)) {
        Section("Panels")
        val panels = settings.orderedPanels
        panels.forEachIndexed { i, ps ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = ps.enabled, onCheckedChange = { on ->
                    update { s -> s.copy(panels = s.orderedPanels.map { if (it.panel == ps.panel) it.copy(enabled = on) else it }) }
                })
                Text(ps.panel.title, Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground, maxLines = 1, fontSize = 15.sp)
                MoveButton("▲", enabled = i > 0) { update { s -> s.copy(panels = s.orderedPanels.swap(i, i - 1)) } }
                MoveButton("▼", enabled = i < panels.size - 1) { update { s -> s.copy(panels = s.orderedPanels.swap(i, i + 1)) } }
            }
        }
        Toggle("Open Workout panel when a workout starts", settings.autoOpenWorkout) { v -> update { it.copy(autoOpenWorkout = v) } }
        Toggle("Open Bike panel when Di2/AXS connects", settings.autoOpenBike) { v -> update { it.copy(autoOpenBike = v) } }

        Section("Big number")
        Choice(BigMetric.entries, settings.bigMetric, { it.label }) { v -> update { it.copy(bigMetric = v) } }
        Section("Zone color")
        Choice(ColorMode.entries, settings.colorMode, { it.label }) { v -> update { it.copy(colorMode = v) } }
        Section("Theme")
        Choice(ThemeMode.entries, settings.theme, { it.label }) { v -> update { it.copy(theme = v) } }

        Section("Ride tiles")
        settings.rideTiles.forEachIndexed { i, m ->
            PickRow("Tile ${i + 1}", m.label) {
                picker = Picker("Tile ${i + 1}", Metric.selectableEntries.map { it.label }) { idx ->
                    val chosen = Metric.selectableEntries[idx]
                    update { s -> s.copy(rideTiles = s.rideTiles.toMutableList().also { it[i] = chosen }) }
                }
            }
        }
        Section("Lap fields")
        settings.lapFields.forEachIndexed { i, m ->
            PickRow("Field ${i + 1}", m.label) {
                picker = Picker("Lap field ${i + 1}", LapMetric.entries.map { it.label }) { idx ->
                    val chosen = LapMetric.entries[idx]
                    update { s -> s.copy(lapFields = s.lapFields.toMutableList().also { it[i] = chosen }) }
                }
            }
        }

        Section("W' balance")
        val ftp = context.graph.hub.profile.collectAsState().value.ftp
        Stepper("CP", settings.criticalPower?.let { "$it W" } ?: "FTP ($ftp W)",
            onMinus = { update { s -> s.copy(criticalPower = ((s.criticalPower ?: ftp) - 5)) } },
            onPlus = { update { s -> s.copy(criticalPower = ((s.criticalPower ?: ftp) + 5)) } },
        )
        if (settings.criticalPower != null) {
            TextButton(onClick = { update { it.copy(criticalPower = null) } }) { Text("Use FTP from the Karoo profile") }
        }
        Stepper("W'", "${settings.wPrimeJoules / 1000.0} kJ",
            onMinus = { update { s -> s.copy(wPrimeJoules = s.wPrimeJoules - 500) } },
            onPlus = { update { s -> s.copy(wPrimeJoules = s.wPrimeJoules + 500) } },
        )

        Section("Workout alerts")
        Choice(AlertStyle.entries, settings.alertStyle, { it.label }) { v -> update { it.copy(alertStyle = v) } }
        Toggle("Beep 10 s before and at each new step", settings.alertBeep) { v -> update { it.copy(alertBeep = v) } }
        Toggle("Countdown beeps 5-4-3-2-1", settings.countdownBeeps) { v -> update { it.copy(countdownBeeps = v) } }
        Toggle("Karoo pop-up on pages without KarooPRO", settings.systemAlert) { v -> update { it.copy(systemAlert = v) } }

        Section("Weather")
        Toggle("Fetch weather (Open-Meteo, needs WiFi or phone)", settings.weatherEnabled) { v -> update { it.copy(weatherEnabled = v) } }
        Spacer(Modifier.height(24.dp))
    }
}

private fun <T> List<T>.swap(a: Int, b: Int): List<T> = toMutableList().also {
    val tmp = it[a]
    it[a] = it[b]
    it[b] = tmp
}

data class Picker(val title: String, val options: List<String>, val onPick: (Int) -> Unit)

@Composable
private fun PickerScreen(p: Picker, close: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(8.dp)) {
        Text(p.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        LazyColumn(Modifier.weight(1f)) {
            items(p.options.indices.toList()) { i ->
                Text(
                    p.options[i],
                    Modifier.fillMaxWidth().clickable {
                        p.onPick(i)
                        close()
                    }.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                HorizontalDivider()
            }
        }
        TextButton(onClick = close) { Text("Cancel") }
    }
}

@Composable
private fun MoveButton(symbol: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, color = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.25f), fontSize = 16.sp)
    }
}

@Composable
private fun Section(title: String) {
    Text(title.uppercase(), Modifier.padding(top = 14.dp, bottom = 4.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
}

@Composable
private fun Toggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun <T> Choice(options: List<T>, selected: T, label: (T) -> String, onPick: (T) -> Unit) {
    options.forEach { o ->
        Row(Modifier.fillMaxWidth().clickable { onPick(o) }, verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = o == selected, onClick = { onPick(o) })
            Text(label(o), color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
        }
    }
}

@Composable
private fun PickRow(label: String, value: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(80.dp), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), fontSize = 14.sp)
        Text(value, Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("›", color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(40.dp), color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
        OutlinedButton(onClick = onMinus) { Text("−") }
        Text(value, Modifier.weight(1f).padding(horizontal = 6.dp), color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
        OutlinedButton(onClick = onPlus) { Text("+") }
    }
}
