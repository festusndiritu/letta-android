package dev.mizzenmast.letta.core.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------
// Font families (system fonts — no certificate required)
// ---------------------------------------------------------------------------

val DmSans       = FontFamily.SansSerif
val Inter        = FontFamily.SansSerif
val Nunito       = FontFamily.SansSerif
val JetBrainsMono = FontFamily.Monospace
val Lato         = FontFamily.SansSerif

// ---------------------------------------------------------------------------
// Presets
// ---------------------------------------------------------------------------

enum class LettaPreset(val label: String) {
    LAPIS("Lapis"),       // deep blue — default
    DUSK("Dusk"),         // warm burgundy/rose
    SAGE("Sage"),         // muted green
    SLATE("Slate"),       // steel blue-grey
    EMBER("Ember"),       // amber-orange
    MONO("Mono"),         // near-black ink
    DYNAMIC("Dynamic"),   // Android 12+ Material You
}

// Extra tokens not in M3 ColorScheme that screens reference
@Immutable
data class LettaExtendedColors(
    val accent: Color,        // primary accent (bubble fill mine, tinted surfaces)
    val accentMid: Color,     // slightly lighter variant
    val accentLight: Color,   // even lighter — for backgrounds
    val accentDim: Color,     // low-opacity overlay
    val bubbleMine: Color,    // outgoing message bubble
    val bubbleTheirs: Color,  // incoming message bubble
    val bg0: Color,           // deepest background
    val bg1: Color,           // main scaffold background
    val bg2: Color,           // surface card
    val bg3: Color,           // elevated surface / input background
    val border: Color,
    val text1: Color,
    val text2: Color,
    val text3: Color,
    val positive: Color,
    val destructive: Color,
    val warning: Color,
)

val LocalLettaColors = staticCompositionLocalOf {
    lightExtendedColors(LapisBase, LapisMid, LapisLight, LapisDim, LapisBubble, LapisBubbleLight)
}

val LocalLettaPreset = staticCompositionLocalOf { LettaPreset.LAPIS }

private fun darkExtendedColors(
    accent: Color, mid: Color, light: Color, dim: Color,
    bubble: Color, bubbleLight: Color,
) = LettaExtendedColors(
    accent = accent, accentMid = mid, accentLight = light, accentDim = dim,
    bubbleMine = bubble,
    bubbleTheirs = DarkBg3,
    bg0 = DarkBg0, bg1 = DarkBg1, bg2 = DarkBg2, bg3 = DarkBg3,
    border = DarkBorder,
    text1 = DarkText1, text2 = DarkText2, text3 = DarkText3,
    positive = PositiveGreen, destructive = DestructiveRed, warning = WarningAmber,
)

private fun lightExtendedColors(
    accent: Color, mid: Color, light: Color, dim: Color,
    bubble: Color, bubbleLight: Color,
) = LettaExtendedColors(
    accent = accent, accentMid = mid, accentLight = light, accentDim = dim,
    bubbleMine = bubble,
    bubbleTheirs = LightBg2,
    bg0 = LightBg0, bg1 = LightBg1, bg2 = LightBg2, bg3 = LightBg3,
    border = LightBorder,
    text1 = LightText1, text2 = LightText2, text3 = LightText3,
    positive = PositiveGreen, destructive = DestructiveRed, warning = WarningAmber,
)

// ---------------------------------------------------------------------------
// M3 color schemes keyed by preset × dark/light
// ---------------------------------------------------------------------------

private fun darkScheme(accent: Color, dim: Color) = darkColorScheme(
    primary = accent,
    onPrimary = Neutral0,
    primaryContainer = dim,
    onPrimaryContainer = accent,
    secondary = Neutral400,
    onSecondary = Neutral950,
    background = DarkBg0,
    onBackground = DarkText1,
    surface = DarkBg1,
    onSurface = DarkText1,
    surfaceVariant = DarkBg3,
    onSurfaceVariant = DarkText2,
    outline = DarkBorder,
    outlineVariant = DarkBg3,
    error = DestructiveRed,
    onError = Neutral0,
    surfaceTint = accent,
    inverseSurface = DarkText1,
    inverseOnSurface = DarkBg1,
)

private fun lightScheme(accent: Color, containerLight: Color, dim: Color) = lightColorScheme(
    primary = accent,
    onPrimary = Neutral0,
    primaryContainer = containerLight,
    onPrimaryContainer = accent,
    secondary = Neutral500,
    onSecondary = Neutral0,
    background = LightBg0,
    onBackground = LightText1,
    surface = LightBg1,
    onSurface = LightText1,
    surfaceVariant = LightBg3,
    onSurfaceVariant = LightText2,
    outline = LightBorder,
    outlineVariant = LightBg2,
    error = DestructiveRed,
    onError = Neutral0,
    surfaceTint = accent,
    inverseSurface = LightText1,
    inverseOnSurface = LightBg0,
)

// ---------------------------------------------------------------------------
// Per-preset color + font resolution
// ---------------------------------------------------------------------------

private fun extendedColors(preset: LettaPreset, dark: Boolean): LettaExtendedColors =
    when (preset) {
        LettaPreset.LAPIS   -> if (dark) darkExtendedColors(LapisBase, LapisMid, LapisLight, LapisDim, LapisBubble, LapisBubbleLight)
                               else lightExtendedColors(LapisBase, LapisMid, LapisLight, LapisDim, LapisBubble, LapisBubbleLight)
        LettaPreset.DUSK    -> if (dark) darkExtendedColors(DuskBase, DuskMid, DuskLight, DuskDim, DuskBubble, DuskBubbleLight)
                               else lightExtendedColors(DuskBase, DuskMid, DuskLight, DuskDim, DuskBubble, DuskBubbleLight)
        LettaPreset.SAGE    -> if (dark) darkExtendedColors(SageBase, SageMid, SageLight, SageDim, SageBubble, SageBubbleLight)
                               else lightExtendedColors(SageBase, SageMid, SageLight, SageDim, SageBubble, SageBubbleLight)
        LettaPreset.SLATE   -> if (dark) darkExtendedColors(SlateBase, SlateMid, SlateLight, SlateDim, SlateBubble, SlateBubbleLight)
                               else lightExtendedColors(SlateBase, SlateMid, SlateLight, SlateDim, SlateBubble, SlateBubbleLight)
        LettaPreset.EMBER   -> if (dark) darkExtendedColors(EmberBase, EmberMid, EmberLight, EmberDim, EmberBubble, EmberBubbleLight)
                               else lightExtendedColors(EmberBase, EmberMid, EmberLight, EmberDim, EmberBubble, EmberBubbleLight)
        LettaPreset.MONO    -> if (dark) darkExtendedColors(MonoBase, MonoMid, MonoLight, MonoDim, MonoBubble, MonoBubbleLight)
                               else lightExtendedColors(MonoBase, MonoMid, MonoLight, MonoDim, MonoBubble, MonoBubbleLight)
        LettaPreset.DYNAMIC -> if (dark) darkExtendedColors(LapisBase, LapisMid, LapisLight, LapisDim, LapisBubble, LapisBubbleLight)
                               else lightExtendedColors(LapisBase, LapisMid, LapisLight, LapisDim, LapisBubble, LapisBubbleLight)
    }

private fun fontFamilyFor(preset: LettaPreset): FontFamily = when (preset) {
    LettaPreset.LAPIS   -> DmSans
    LettaPreset.DUSK    -> Lato
    LettaPreset.SAGE    -> Nunito
    LettaPreset.SLATE   -> Inter
    LettaPreset.EMBER   -> Inter
    LettaPreset.MONO    -> JetBrainsMono
    LettaPreset.DYNAMIC -> DmSans
}

private fun lettaTypography(ff: FontFamily) = androidx.compose.material3.Typography(
    displayLarge  = TextStyle(fontFamily = ff, fontWeight = FontWeight.Normal,   fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium = TextStyle(fontFamily = ff, fontWeight = FontWeight.Normal,   fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall  = TextStyle(fontFamily = ff, fontWeight = FontWeight.Normal,   fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge  = TextStyle(fontFamily = ff, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = ff, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall  = TextStyle(fontFamily = ff, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge  = TextStyle(fontFamily = ff, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = ff, fontWeight = FontWeight.Medium,   fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall  = TextStyle(fontFamily = ff, fontWeight = FontWeight.Medium,   fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge   = TextStyle(fontFamily = ff, fontWeight = FontWeight.Normal,   fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium  = TextStyle(fontFamily = ff, fontWeight = FontWeight.Normal,   fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall   = TextStyle(fontFamily = ff, fontWeight = FontWeight.Normal,   fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge  = TextStyle(fontFamily = ff, fontWeight = FontWeight.Medium,   fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = ff, fontWeight = FontWeight.Medium,   fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall  = TextStyle(fontFamily = ff, fontWeight = FontWeight.Medium,   fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)

// ---------------------------------------------------------------------------
// LettaTheme
// ---------------------------------------------------------------------------

@Composable
fun LettaTheme(
    preset: LettaPreset = LettaPreset.LAPIS,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    val colorScheme = when {
        preset == LettaPreset.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> darkScheme(
            accent = extendedColors(preset, true).accent,
            dim    = extendedColors(preset, true).accentDim,
        )
        else -> lightScheme(
            accent         = extendedColors(preset, false).accent,
            containerLight = extendedColors(preset, false).accentLight,
            dim            = extendedColors(preset, false).accentDim,
        )
    }

    CompositionLocalProvider(
        LocalLettaPreset provides preset,
        LocalLettaColors provides extendedColors(preset, darkTheme),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = lettaTypography(fontFamilyFor(preset)),
            content     = content,
        )
    }
}

// Convenience accessor
val lc @Composable get() = LocalLettaColors.current