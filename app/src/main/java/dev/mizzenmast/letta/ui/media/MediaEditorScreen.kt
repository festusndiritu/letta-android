package dev.mizzenmast.letta.ui.media

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage

// ── Draggable text/emoji overlay ─────────────────────────────────────────────

data class TextOverlayItem(
    val id: String,
    val text: String,
    val x: Float = 0f,
    val y: Float = 0f,
    val color: Color = Color.White,
    val fontSize: Float = 20f,
)

/**
 * Media editor screen.
 *
 * Supports:
 *  - Image: preview with rotation (90° steps), draggable text/emoji overlays
 *  - Video: playback preview with trim range slider
 *  - Caption text field
 *  - Send / cancel actions
 *
 * Crop is shown as an icon action but delegates the actual crop operation to
 * the caller via [onCropRequested] since a full crop UI requires a separate
 * canvas-based view.
 *
 * @param uri          The media URI to edit
 * @param mimeType     MIME type string (e.g. "image/jpeg", "video/mp4")
 * @param onSend       Called with (uri, caption) when user confirms send
 * @param onCancel     Called when user presses back
 * @param onCropRequested  Called to launch an external crop flow
 */
@Composable
fun MediaEditorScreen(
    uri: Uri,
    mimeType: String?,
    onSend: (uri: Uri, caption: String) -> Unit,
    onCancel: () -> Unit,
    onCropRequested: (uri: Uri) -> Unit = {},
) {
    val isVideo = mimeType?.startsWith("video") == true

    var caption by remember { mutableStateOf("") }
    var rotationDegrees by remember { mutableFloatStateOf(0f) }
    var showTextInput by remember { mutableStateOf(false) }
    var pendingOverlayText by remember { mutableStateOf("") }
    val overlays = remember { mutableStateListOf<TextOverlayItem>() }

    // Video trim state (0f..1f normalized)
    var trimStart by remember { mutableFloatStateOf(0f) }
    var trimEnd by remember { mutableFloatStateOf(1f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // ── Media preview ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (isVideo) 16f / 9f else 1f)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(8.dp)),
        ) {
            if (isVideo) {
                VideoPreview(uri = uri)
            } else {
                AsyncImage(
                    model = uri,
                    contentDescription = "Media preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(rotationDegrees),
                )
            }

            // Draggable text overlays
            overlays.forEachIndexed { index, item ->
                DraggableTextOverlay(
                    item = item,
                    onMove = { dx, dy ->
                        overlays[index] = item.copy(x = item.x + dx, y = item.y + dy)
                    },
                    onRemove = { overlays.removeAt(index) },
                )
            }
        }

        // ── Top bar ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!isVideo) {
                    IconButton(onClick = { rotationDegrees = (rotationDegrees + 90f) % 360f }) {
                        Icon(Icons.Rounded.RotateRight, contentDescription = "Rotate", tint = Color.White)
                    }
                    IconButton(onClick = { onCropRequested(uri) }) {
                        Icon(Icons.Rounded.Crop, contentDescription = "Crop", tint = Color.White)
                    }
                }
                IconButton(onClick = { showTextInput = !showTextInput }) {
                    Icon(Icons.Rounded.TextFields, contentDescription = "Add text", tint = Color.White)
                }
                IconButton(onClick = {
                    // Quick emoji insert — adds a fixed emoji overlay
                    overlays.add(
                        TextOverlayItem(
                            id = System.nanoTime().toString(),
                            text = "😊",
                            fontSize = 32f,
                        )
                    )
                }) {
                    Icon(Icons.Rounded.EmojiEmotions, contentDescription = "Add emoji", tint = Color.White)
                }
            }
        }

        // ── Video trim slider ─────────────────────────────────────────────
        if (isVideo) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 16.dp)
                    .offset(y = 160.dp),
            ) {
                Text(
                    text = "Trim (max 60s)",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                VideoTrimSlider(
                    trimStart = trimStart,
                    trimEnd = trimEnd,
                    onTrimStartChange = { trimStart = it.coerceAtMost(trimEnd - 0.05f) },
                    onTrimEndChange = { trimEnd = it.coerceAtLeast(trimStart + 0.05f) },
                )
            }
        }

        // ── Bottom: caption + send ────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.5f))
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Text overlay input
            AnimatedVisibility(visible = showTextInput, enter = fadeIn(), exit = fadeOut()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = pendingOverlayText,
                        onValueChange = { pendingOverlayText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Add text overlay", color = Color.White.copy(alpha = 0.5f)) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                        ),
                    )
                    FilledTonalButton(
                        onClick = {
                            if (pendingOverlayText.isNotBlank()) {
                                overlays.add(
                                    TextOverlayItem(
                                        id = System.nanoTime().toString(),
                                        text = pendingOverlayText,
                                    )
                                )
                                pendingOverlayText = ""
                                showTextInput = false
                            }
                        },
                    ) {
                        Text("Add")
                    }
                }
            }

            // Caption field
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Add a caption", color = Color.White.copy(alpha = 0.5f)) },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                    ),
                    shape = RoundedCornerShape(24.dp),
                )
                IconButton(
                    onClick = { onSend(uri, caption) },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

// ── Video preview (ExoPlayer) ─────────────────────────────────────────────────

@Composable
private fun VideoPreview(uri: Uri) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = ExoPlayer.REPEAT_MODE_ALL
            prepare()
        }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = true
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

// ── Video trim slider ─────────────────────────────────────────────────────────

@Composable
private fun VideoTrimSlider(
    trimStart: Float,
    trimEnd: Float,
    onTrimStartChange: (Float) -> Unit,
    onTrimEndChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Start", color = Color.White, style = MaterialTheme.typography.labelSmall)
            Text("End", color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
        Slider(
            value = trimStart,
            onValueChange = onTrimStartChange,
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth(),
        )
        Slider(
            value = trimEnd,
            onValueChange = onTrimEndChange,
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ── Draggable text/emoji overlay composable ───────────────────────────────────

@Composable
private fun DraggableTextOverlay(
    item: TextOverlayItem,
    onMove: (dx: Float, dy: Float) -> Unit,
    onRemove: () -> Unit,
) {
    Box(
        modifier = Modifier
            .wrapContentSize()
            .graphicsLayer {
                translationX = item.x
                translationY = item.y
            }
            .pointerInput(item.id) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onMove(dragAmount.x, dragAmount.y)
                }
            },
    ) {
        Text(
            text = item.text,
            color = item.color,
            fontSize = item.fontSize.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(4.dp)
                .clickable { onRemove() }, // tap to remove
        )
    }
}

// Helper extension for Box offset
private fun androidx.compose.ui.Modifier.offset(y: androidx.compose.ui.unit.Dp) =
    this.padding(top = y)
