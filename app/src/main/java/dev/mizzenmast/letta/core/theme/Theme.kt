package dev.mizzenmast.letta.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------
// Presets
// ---------------------------------------------------------------------------

enum class LettaPreset(val label: String) {
    DEFAULT("Default"),
    DUSK("Dusk"),
    MONO("Mono"),
    SLATE("Slate"),
    FOREST("Forest"),
}

data class LettaColors(
    val primary: Color,
    val primaryContainer: Color,
    val onPrimary: Color,
    val surfaceTint: Color,
)

val presetColors = mapOf(
    LettaPreset.DEFAULT to LettaColors(
        primary = Color(0xFF4A90D9),
        primaryContainer = Color(0xFFD6EAFF),
        onPrimary = Color.White,
        surfaceTint = Color(0xFF4A90D9),
    ),
    LettaPreset.DUSK to LettaColors(
        primary = Color(0xFF9B6B6B),
        primaryContainer = Color(0xFFFFDAD9),
        onPrimary = Color.White,
        surfaceTint = Color(0xFF9B6B6B),
    ),
    LettaPreset.MONO to LettaColors(
        primary = Color(0xFF5B7FA6),
        primaryContainer = Color(0xFFD3E4F7),
        onPrimary = Color.White,
        surfaceTint = Color(0xFF5B7FA6),
    ),
    LettaPreset.SLATE to LettaColors(
        primary = Color(0xFF6B7A8D),
        primaryContainer = Color(0xFFDDE3EC),
        onPrimary = Color.White,
        surfaceTint = Color(0xFF6B7A8D),
    ),
    LettaPreset.FOREST to LettaColors(
        primary = Color(0xFF5A8A6A),
        primaryContainer = Color(0xFFCCE8D4),
        onPrimary = Color.White,
        surfaceTint = Color(0xFF5A8A6A),
    ),
)

val presetFonts = mapOf(
    LettaPreset.DEFAULT to FontChoice.DM_SANS,
    LettaPreset.DUSK to FontChoice.LATO,
    LettaPreset.MONO to FontChoice.JETBRAINS_MONO,
    LettaPreset.SLATE to FontChoice.NUNITO,
    LettaPreset.FOREST to FontChoice.NUNITO,
)

// ---------------------------------------------------------------------------
// Fonts
// ---------------------------------------------------------------------------

enum class FontChoice {
    DM_SANS,
    LATO,
    JETBRAINS_MONO,
    NUNITO,
}

// Google Fonts loaded at runtime — fallback to system sans
val dmSans = FontFamily.Default
val lato = FontFamily.Default
val jetBrainsMono = FontFamily.Monospace
val nunito = FontFamily.Default

fun fontFamilyFor(choice: FontChoice): FontFamily = when (choice) {
    FontChoice.DM_SANS -> dmSans
    FontChoice.LATO -> lato
    FontChoice.JETBRAINS_MONO -> jetBrainsMono
    FontChoice.NUNITO -> nunito
}

fun lettaTypography(fontFamily: FontFamily) = androidx.compose.material3.Typography(
    displayLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)

// ---------------------------------------------------------------------------
// Color schemes
// ---------------------------------------------------------------------------

fun darkScheme(c: LettaColors) = darkColorScheme(
    primary = c.primary,
    onPrimary = c.onPrimary,
    primaryContainer = c.primary.copy(alpha = 0.2f),
    onPrimaryContainer = c.primary,
    secondary = Color(0xFF90A8C0),
    onSecondary = Color(0xFF1A2B38),
    background = Color(0xFF0D0D0F),
    onBackground = Color(0xFFE4E4EF),
    surface = Color(0xFF141416),
    onSurface = Color(0xFFE4E4EF),
    surfaceVariant = Color(0xFF1E1E22),
    onSurfaceVariant = Color(0xFFA0A0B8),
    outline = Color(0xFF3A3A45),
    outlineVariant = Color(0xFF2A2A33),
    inverseSurface = Color(0xFFE4E4EF),
    inverseOnSurface = Color(0xFF141416),
    surfaceTint = c.surfaceTint,
)

fun lightScheme(c: LettaColors) = lightColorScheme(
    primary = c.primary,
    onPrimary = c.onPrimary,
    primaryContainer = c.primaryContainer,
    onPrimaryContainer = c.primary.copy(alpha = 0.8f),
    secondary = Color(0xFF5B7FA6),
    onSecondary = Color.White,
    background = Color(0xFFF8F8FC),
    onBackground = Color(0xFF111118),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111118),
    surfaceVariant = Color(0xFFF0F0F6),
    onSurfaceVariant = Color(0xFF505060),
    outline = Color(0xFFCCCCD8),
    outlineVariant = Color(0xFFE4E4EF),
    inverseSurface = Color(0xFF111118),
    inverseOnSurface = Color(0xFFF8F8FC),
    surfaceTint = c.surfaceTint,
)

// ---------------------------------------------------------------------------
// CompositionLocal for current preset
// ---------------------------------------------------------------------------

val LocalLettaPreset = staticCompositionLocalOf { LettaPreset.DEFAULT }

// ---------------------------------------------------------------------------
// LettaTheme
// ---------------------------------------------------------------------------

@Composable
fun LettaTheme(
    preset: LettaPreset = LettaPreset.DEFAULT,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = presetColors[preset] ?: presetColors[LettaPreset.DEFAULT]!!
    val fontChoice = presetFonts[preset] ?: FontChoice.DM_SANS
    val fontFamily = fontFamilyFor(fontChoice)

    val colorScheme = if (darkTheme) darkScheme(colors) else lightScheme(colors)
    val typography = lettaTypography(fontFamily)

    CompositionLocalProvider(LocalLettaPreset provides preset) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content,
        )
    }
}