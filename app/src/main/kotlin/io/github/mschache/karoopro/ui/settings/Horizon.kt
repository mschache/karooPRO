package io.github.mschache.karoopro.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.mschache.karoopro.R

/**
 * Pangu Horizon (~/Pangu/brand, pangu-brand 1.3.0) for the app's own screens: ink and paper do the work, signal orange
 * is the one accent. The field keeps its own look: zone colors are data, and next to them nothing else is orange.
 */
object Horizon {
    val Ink = Color(0xFF111111)
    val Paper = Color(0xFFF5F4F0)
    val Signal = Color(0xFFFF4F1A)
    val Cell = Color(0xFF1C1C1B)
    val Fill = Color(0xFF2A2A28)
    val Line = Color(0x29F5F4F0)
    val Muted = Color(0xFFAEACA4)
    val Second = Color(0xFF8A877F)

    /** Archivo Expanded Black Italic: headlines only, always in capitals. */
    val Display = FontFamily(Font(R.font.archivo_expanded_black_italic, FontWeight.Black, FontStyle.Italic))

    /** IBM Plex Mono Medium: labels and data, in capitals with wide tracking. */
    val Mono = FontFamily(Font(R.font.ibm_plex_mono_medium, FontWeight.Medium))

    /** Inter Tight 400 and 600 from the variable file: everything else, in sentence case. */
    @OptIn(ExperimentalTextApi::class)
    val Text = FontFamily(
        Font(R.font.inter_tight, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
        Font(R.font.inter_tight, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
        Font(R.font.inter_tight, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    )
}

@Composable
fun HorizonTheme(content: @Composable () -> Unit) {
    val base = Typography()
    // Line height follows the size, so a smaller fontSize on a Text doesn't keep the style's tall lines.
    fun TextStyle.text() = copy(fontFamily = Horizon.Text, lineHeight = 1.3.em)
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Horizon.Signal,
            onPrimary = Horizon.Ink,
            secondary = Horizon.Paper,
            onSecondary = Horizon.Ink,
            background = Horizon.Ink,
            onBackground = Horizon.Paper,
            surface = Horizon.Ink,
            onSurface = Horizon.Paper,
            surfaceVariant = Horizon.Cell,
            onSurfaceVariant = Horizon.Muted,
            surfaceContainer = Horizon.Cell,
            surfaceContainerHigh = Horizon.Fill,
            outline = Horizon.Line,
            outlineVariant = Horizon.Line,
        ),
        typography = Typography(
            displayLarge = base.displayLarge.text(), displayMedium = base.displayMedium.text(), displaySmall = base.displaySmall.text(),
            headlineLarge = base.headlineLarge.text(), headlineMedium = base.headlineMedium.text(), headlineSmall = base.headlineSmall.text(),
            titleLarge = base.titleLarge.text(), titleMedium = base.titleMedium.text(), titleSmall = base.titleSmall.text(),
            bodyLarge = base.bodyLarge.text(), bodyMedium = base.bodyMedium.text(), bodySmall = base.bodySmall.text(),
            labelLarge = base.labelLarge.text(), labelMedium = base.labelMedium.text(), labelSmall = base.labelSmall.text(),
        ),
        content = content,
    )
}

/** A headline in the display face: capitals, tight tracking and leading. */
@Composable
fun DisplayText(text: String, size: TextUnit, modifier: Modifier = Modifier, color: Color = Horizon.Paper) {
    Text(
        text.uppercase(), modifier, color = color, fontFamily = Horizon.Display, fontWeight = FontWeight.Black,
        fontStyle = FontStyle.Italic, fontSize = size, letterSpacing = (-0.02).em, lineHeight = size * 0.95f,
    )
}

/** A label in the mono face: capitals, tracked 0.14 em. */
@Composable
fun MonoLabel(text: String, modifier: Modifier = Modifier, color: Color = Horizon.Muted, size: TextUnit = 11.sp) {
    Text(text.uppercase(), modifier, color = color, fontFamily = Horizon.Mono, fontSize = size, letterSpacing = 0.14.em, maxLines = 1)
}

/** The horizon line: the one orange rule under a header. */
@Composable
fun HorizonLine(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(2.dp).background(Horizon.Signal))
}
