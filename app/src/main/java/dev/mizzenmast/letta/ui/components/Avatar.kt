package dev.mizzenmast.letta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.text.BreakIterator

// ---------------------------------------------------------------------------
// Deterministic colour palette for avatar backgrounds
// ---------------------------------------------------------------------------
private val AvatarPalette = listOf(
    Color(0xFF3A6FD8), // blue
    Color(0xFF8A2E49), // rose
    Color(0xFF3D7A5C), // sage
    Color(0xFF2A4468), // slate
    Color(0xFF8F4A00), // ember
    Color(0xFF6B4EAA), // violet
    Color(0xFF1A7A8A), // teal
    Color(0xFF7A5C2E), // caramel
    Color(0xFF3A6050), // forest
    Color(0xFF8A3A6F), // plum
)

/**
 * Returns a stable colour for a given name string. The hash is computed from
 * the full raw string so different names with identical initials still get
 * different colours.
 */
fun avatarColorFor(name: String): Color {
    val hash = name.fold(0) { acc, c -> acc * 31 + c.code }
    return AvatarPalette[Math.floorMod(hash, AvatarPalette.size)]
}

/**
 * Extracts the display character(s) for an avatar from an arbitrary display
 * name. Handles:
 *   - Empty strings                     → "?"
 *   - Emoji at start (single grapheme)  → that emoji (e.g. "🐱 Kitty" → "🐱")
 *   - ASCII/Latin names                 → uppercased first letter
 *   - Ideographic scripts               → first character
 *   - Names starting with spaces/punct  → first non-whitespace grapheme
 */
fun avatarInitial(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return "?"

    val it = BreakIterator.getCharacterInstance()
    it.setText(trimmed)
    val start = it.first()
    val end = it.next()
    if (end == BreakIterator.DONE) return "?"

    val grapheme = trimmed.substring(start, end)

    // If the first grapheme is an emoji (detected by codepoint > 0xFFFF or
    // known emoji ranges), return it as-is.
    val cp = grapheme.codePointAt(0)
    val isEmoji = cp > 0xFFFF ||
        (cp in 0x2600..0x27BF) ||  // Misc symbols & dingbats
        (cp in 0x1F300..0x1FAFF)    // Supplemental symbols, emoticons, etc.

    if (isEmoji) return grapheme

    // Otherwise return the uppercased first character
    return grapheme.uppercase()
}

// ---------------------------------------------------------------------------
// Composable
// ---------------------------------------------------------------------------

/**
 * Circular avatar that:
 *   - Shows [imageUrl] via Coil when non-null / non-blank
 *   - Falls back to coloured initial based on [name]
 *
 * @param size      Diameter of the circle
 * @param fontSize  Overrides automatic scaling; leave null for auto
 */
@Composable
fun LettaAvatar(
    name: String,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    size: Dp = 44.dp,
    fontSize: TextUnit = TextUnit.Unspecified,
) {
    val initial = remember(name) { avatarInitial(name) }
    val bg      = remember(name) { avatarColorFor(name) }
    val autoFontSize = remember(size) { (size.value * 0.42f).sp }
    val effectiveFontSize = if (fontSize == TextUnit.Unspecified) autoFontSize else fontSize

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bg),
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize().clip(CircleShape),
                // On error, the coloured initial underneath shows through
                // because the AsyncImage is overlaid on the Box background.
                onError = { /* no-op, fallback is the bg+text below */ },
            )
        }
        // Always render the initial underneath so it shows if image fails
        Text(
            text = initial,
            fontSize = effectiveFontSize,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
