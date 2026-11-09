package de.schnurrfeld.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Accent = Color(0xFFE8846B)
val AccentDark = Color(0xFFF09A82)
val Heart = Color(0xFFE5484D)
val Gold = Color(0xFFF5B83D)

private val Light = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFCE3DB),
    onPrimaryContainer = Color(0xFF5A2213),
    secondary = Color(0xFF5B6B8C),
    secondaryContainer = Color(0xFFE6EAF3),
    onSecondaryContainer = Color(0xFF1F2A40),
    background = Color(0xFFF3EADB),
    onBackground = Color(0xFF2A2420),
    surface = Color(0xFFFBF6EE),
    onSurface = Color(0xFF2A2420),
    surfaceVariant = Color(0xFFEDE2D0),
    onSurfaceVariant = Color(0xFF6E6358),
    surfaceContainer = Color(0xFFFBF6EE),
    surfaceContainerHigh = Color(0xFFFDF9F3),
    outline = Color(0xFFD9CAB3),
    outlineVariant = Color(0xFFE8DCC9),
)

private val Dark = darkColorScheme(
    primary = AccentDark,
    onPrimary = Color(0xFF2A0F07),
    primaryContainer = Color(0xFF4A271C),
    onPrimaryContainer = Color(0xFFFFDBD0),
    secondary = Color(0xFFA9B6D3),
    secondaryContainer = Color(0xFF2A3142),
    onSecondaryContainer = Color(0xFFDDE4F5),
    background = Color(0xFF121214),
    onBackground = Color(0xFFE8E6E3),
    surface = Color(0xFF1C1C20),
    onSurface = Color(0xFFE8E6E3),
    surfaceVariant = Color(0xFF26262B),
    onSurfaceVariant = Color(0xFFA9A6A1),
    surfaceContainer = Color(0xFF1C1C20),
    surfaceContainerHigh = Color(0xFF232328),
    outline = Color(0xFF3A3A40),
    outlineVariant = Color(0xFF2C2C31),
)

val LocalDark = staticCompositionLocalOf { false }

private val AppTypography = Typography().let { t ->
    t.copy(
        displaySmall = t.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.Bold),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun SchnurrTheme(dark: Boolean, content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(LocalDark provides dark) {
        MaterialTheme(colorScheme = if (dark) Dark else Light, typography = AppTypography, content = content)
    }
}

/**
 * Region palette – 12 hues spread around the wheel with varied lightness.
 * Order matters: the first entries are the most distinct, small boards use them.
 */
object RegionPalette {
    private val light = listOf(
        0xFFFF8A80, // coral
        0xFF64B5F6, // blue
        0xFFFFEE58, // yellow
        0xFF81C784, // green
        0xFFCE93D8, // purple
        0xFF80DEEA, // cyan
        0xFFFFB74D, // orange
        0xFFF48FB1, // pink
        0xFFD4E157, // lime
        0xFF9FA8DA, // lavender
        0xFFC8A27E, // tan
        0xFFCFD8DC, // grey
    ).map { Color(it) }

    private val dark = listOf(
        0xFFC4574F, 0xFF3B7CC0, 0xFFC4AE2E, 0xFF3F9150, 0xFF975FA6, 0xFF3AA2AE,
        0xFFC7822D, 0xFFBE5A7C, 0xFF8FA22E, 0xFF6670B5, 0xFF92704F, 0xFF6C7A82,
    ).map { Color(it) }

    fun color(slot: Int, dark: Boolean) = (if (dark) this.dark else light)[slot % 12]
    fun pattern(slot: Int) = slot % 12
}

/** Fur colours for the cats. */
data class Fur(val base: Color, val shade: Color, val belly: Color, val patches: Color? = null, val stripes: Boolean = false)

val Furs = listOf(
    Fur(Color(0xFFF4A259), Color(0xFFC97A35), Color(0xFFFFE3C4), stripes = true),   // ginger tabby
    Fur(Color(0xFFA9ACB8), Color(0xFF7D8090), Color(0xFFE9EAF0), stripes = true),   // grey tabby
    Fur(Color(0xFFFFFBF5), Color(0xFFD9D2C7), Color(0xFFFFFFFF)),                   // white
    Fur(Color(0xFF3F3F4A), Color(0xFF26262E), Color(0xFF6A6A78)),                   // black
    Fur(Color(0xFFF3E1C2), Color(0xFFCDB48C), Color(0xFFFFF6E6)),                   // cream
    Fur(Color(0xFFFFFBF5), Color(0xFFD9D2C7), Color(0xFFFFFFFF), patches = Color(0xFFF0A050)), // calico-ish
    Fur(Color(0xFFA57452), Color(0xFF7A5034), Color(0xFFE6CDB8), stripes = true),   // brown tabby
)
