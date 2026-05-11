package dev.mizzenmast.letta.ui.chat

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.emoji2.text.EmojiCompat
import dev.mizzenmast.letta.data.remote.dto.MessageDto
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sin

// ─── Text / Time Utilities ────────────────────────────────────────────────────

internal fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}

internal fun formatPlaybackTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}

internal fun formatLastSeen(epochMs: Long): String {
    val zoned = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault())
    val today = java.time.LocalDate.now(zoned.zone)
    return if (zoned.toLocalDate() == today) {
        zoned.format(DateTimeFormatter.ofPattern("HH:mm"))
    } else {
        zoned.format(DateTimeFormatter.ofPattern("MMM d, HH:mm"))
    }
}

internal fun formatCreatedAt(createdAt: String): String {
    return runCatching {
        val zoned = Instant.parse(createdAt).atZone(ZoneId.systemDefault())
        zoned.format(DateTimeFormatter.ofPattern("MMM d, HH:mm"))
    }.getOrElse { "" }
}

// ─── Emoji Utilities ──────────────────────────────────────────────────────────

internal fun emojiForInsert(emoji: String): String {
    return EmojiCompat.get().process(emoji).toString()
}

internal fun removeLastGlyph(text: String): String {
    if (text.isEmpty()) return text
    val end = text.length
    val count = text.codePointCount(0, end)
    if (count <= 0) return ""
    val newEnd = text.offsetByCodePoints(0, count - 1)
    return text.substring(0, newEnd)
}

// ─── Audio Utilities ──────────────────────────────────────────────────────────

internal fun waveformHeights(seed: String, count: Int): List<Float> {
    if (count <= 0) return emptyList()
    val base = (seed.hashCode() % 1000) / 1000.0
    return List(count) { index ->
        val value = abs(sin((index + 1) * 0.7 + base))
        (0.35 + (value * 0.65)).toFloat()
    }
}

internal fun audioLabel(mimeType: String?): String {
    val mime = mimeType?.lowercase().orEmpty()
    return when {
        mime.contains("mpeg") -> "Audio • MP3"
        mime.contains("ogg") -> "Audio • OGG"
        mime.contains("mp4") -> "Audio • MP4"
        mime.contains("webm") -> "Audio • WebM"
        else -> "Audio"
    }
}

// ─── Document Utilities ───────────────────────────────────────────────────────

internal data class DocumentMeta(val label: String, val icon: ImageVector)

internal fun documentInfo(mimeType: String?, url: String?): DocumentMeta {
    val mime = mimeType?.lowercase().orEmpty()
    return when {
        mime.contains("pdf") || url?.endsWith(".pdf", true) == true ->
            DocumentMeta("PDF", Icons.Rounded.PictureAsPdf)
        mime == "application/msword" || mime.contains("wordprocessingml") ||
            url?.endsWith(".doc", true) == true || url?.endsWith(".docx", true) == true ->
            DocumentMeta("Word", Icons.AutoMirrored.Rounded.Article)
        mime == "text/plain" || url?.endsWith(".txt", true) == true ->
            DocumentMeta("Text", Icons.Rounded.Description)
        else -> DocumentMeta("Document", Icons.AutoMirrored.Rounded.InsertDriveFile)
    }
}

// ─── URL / Link Preview Utilities ────────────────────────────────────────────

internal fun extractFirstUrl(text: String?): String? {
    if (text.isNullOrBlank()) return null
    val regex = Regex("""((https?://)?([A-Za-z0-9-]+\.)+[A-Za-z]{2,}[^\s]*)""")
    val match = regex.find(text) ?: return null
    return match.value.trim().trimEnd('.', ',', '!', '?', ')', ']', '}', '"', '\'')
}

internal fun normalizeUrl(raw: String): String {
    return if (raw.startsWith("http://", true) || raw.startsWith("https://", true)) {
        raw
    } else {
        "https://$raw"
    }
}

internal fun shouldPreviewLink(url: String, fullText: String?): Boolean {
    val trimmed = fullText?.trim().orEmpty()
    val cleaned = url.trim().trimEnd('.', ',', '!', '?', ')', ']', '}', '"', '\'')
    val hasScheme = cleaned.startsWith("http://", true) || cleaned.startsWith("https://", true)
    val hasWww = cleaned.startsWith("www.", true)
    return hasScheme || hasWww || trimmed.equals(cleaned, ignoreCase = false)
}

// ─── Search / Message Utilities ───────────────────────────────────────────────

internal fun searchPreview(message: MessageDto): String {
    val content = message.content?.trim().orEmpty()
    if (content.isNotEmpty()) return content
    return when (message.type) {
        "image" -> "Photo"
        "video" -> "Video"
        "audio" -> "Audio"
        "document" -> "Document"
        else -> "Message"
    }
}

// ─── Media Utilities ──────────────────────────────────────────────────────────

internal fun cacheBitmap(bitmap: Bitmap, cacheDir: File): Uri? {
    return try {
        val file = File(cacheDir, "camera_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        Uri.fromFile(file)
    } catch (_: Exception) {
        null
    }
}
