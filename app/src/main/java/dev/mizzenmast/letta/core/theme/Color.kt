package dev.mizzenmast.letta.core.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Neutral surface layers  (dark)
// ---------------------------------------------------------------------------
val Neutral950 = Color(0xFF0A0A0C)
val Neutral900 = Color(0xFF111114)
val Neutral850 = Color(0xFF18181C)
val Neutral800 = Color(0xFF1E1E24)
val Neutral750 = Color(0xFF25252C)
val Neutral700 = Color(0xFF2C2C35)
val Neutral600 = Color(0xFF3C3C48)
val Neutral500 = Color(0xFF555564)
val Neutral400 = Color(0xFF7878A0)
val Neutral300 = Color(0xFFA8A8C4)
val Neutral200 = Color(0xFFCCCCE0)
val Neutral100 = Color(0xFFE8E8F4)
val Neutral50  = Color(0xFFF4F4FA)
val Neutral0   = Color(0xFFFFFFFF)

// ---------------------------------------------------------------------------
// Accent palettes — each preset provides a full swatch so both schemes look
// distinct. Defined as plain Color constants so there's no runtime allocation.
// ---------------------------------------------------------------------------

// ── Lapis (default — deep blue) ──────────────────────────────────────────────
val LapisBase        = Color(0xFF3A6FD8)
val LapisMid         = Color(0xFF5B8EF0)
val LapisLight       = Color(0xFF7AAAF5)
val LapisDim         = Color(0x1A3A6FD8)
val LapisBubble      = Color(0xFF2557C5)
val LapisBubbleLight = Color(0xFFD8E8FF)

// ── Dusk (warm burgundy / rose) ──────────────────────────────────────────────
val DuskBase        = Color(0xFFA14460)
val DuskMid         = Color(0xFFBF6A82)
val DuskLight       = Color(0xFFD98FA4)
val DuskDim         = Color(0x1AA14460)
val DuskBubble      = Color(0xFF8A2E49)
val DuskBubbleLight = Color(0xFFFFE0EA)

// ── Sage (muted green) ───────────────────────────────────────────────────────
val SageBase        = Color(0xFF3D7A5C)
val SageMid         = Color(0xFF5E9A7A)
val SageLight       = Color(0xFF88C4A6)
val SageDim         = Color(0x1A3D7A5C)
val SageBubble      = Color(0xFF2A6046)
val SageBubbleLight = Color(0xFFD0EDDF)

// ── Slate (cool steel blue-grey) ─────────────────────────────────────────────
val SlateBase        = Color(0xFF3D5A80)
val SlateMid         = Color(0xFF5A7AA0)
val SlateLight       = Color(0xFF8CAAC8)
val SlateDim         = Color(0x1A3D5A80)
val SlateBubble      = Color(0xFF2A4468)
val SlateBubbleLight = Color(0xFFD4E2F5)

// ── Ember (amber-orange) ──────────────────────────────────────────────────────
val EmberBase        = Color(0xFFC06A18)
val EmberMid         = Color(0xFFD98840)
val EmberLight       = Color(0xFFF0B06A)
val EmberDim         = Color(0x1AC06A18)
val EmberBubble      = Color(0xFF8F4A00)
val EmberBubbleLight = Color(0xFFFFE8CC)

// ── Mono (near-black / near-white ink) ───────────────────────────────────────
val MonoBase        = Color(0xFF303040)
val MonoMid         = Color(0xFF505068)
val MonoLight       = Color(0xFF8888A8)
val MonoDim         = Color(0x1A303040)
val MonoBubble      = Color(0xFF1C1C2A)
val MonoBubbleLight = Color(0xFFE8E8F0)

// ---------------------------------------------------------------------------
// Semantic surfaces (shared across all dark themes)
// ---------------------------------------------------------------------------
val DarkBg0    = Neutral950
val DarkBg1    = Neutral900
val DarkBg2    = Neutral850
val DarkBg3    = Neutral800
val DarkBorder = Neutral700
val DarkText1  = Neutral100
val DarkText2  = Neutral300
val DarkText3  = Neutral500

// Semantic surfaces (shared across all light themes)
val LightBg0    = Neutral0
val LightBg1    = Neutral50
val LightBg2    = Neutral100
val LightBg3    = Color(0xFFEEEEF8)
val LightBorder = Neutral200
val LightText1  = Neutral900
val LightText2  = Neutral600
val LightText3  = Neutral400

// Functional
val PositiveGreen = Color(0xFF34C77A)
val DestructiveRed = Color(0xFFE55454)
val WarningAmber = Color(0xFFF59E0B)
val InfoBlue = Color(0xFF3A8EF6)
