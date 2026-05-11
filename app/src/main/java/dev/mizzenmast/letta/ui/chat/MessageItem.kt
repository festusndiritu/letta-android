package dev.mizzenmast.letta.ui.chat

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import dev.mizzenmast.letta.data.local.entity.MessageEntity
import dev.mizzenmast.letta.data.remote.dto.LinkPreviewDto
import dev.mizzenmast.letta.core.motion.HapticFeedback
import dev.mizzenmast.letta.core.motion.rememberHapticFeedback
import dev.mizzenmast.letta.ui.components.LettaBubble
import dev.mizzenmast.letta.ui.components.BubbleText
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.max

// ─── MessageItem ─────────────────────────────────────────────────────────────

@Composable
internal fun MessageItem(
    message: MessageEntity,
    showSenderName: Boolean,
    replyMessage: MessageEntity?,
    reactionCounts: Map<String, Int>,
    showReactionPicker: Boolean,
    reactionPresets: List<String>,
    onReact: (String) -> Unit,
    onLongPress: () -> Unit,
    onSwipeReply: () -> Unit,
    onImageClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    swipeThresholdPx: Float,
    linkPreviews: Map<String, LinkPreviewDto>,
    onRequestLinkPreview: (String) -> Unit,
) {
    val haptics = rememberHapticFeedback()

    Column(modifier = Modifier.fillMaxWidth()) {
        LettaBubble(
            isMine = message.isMine,
            content = {
                MessageBubbleContent(
                    message = message,
                    replyMessage = replyMessage,
                    onImageClick = onImageClick,
                    onVideoClick = onVideoClick,
                    linkPreviews = linkPreviews,
                    onRequestLinkPreview = onRequestLinkPreview,
                )
            },
            showSenderName = showSenderName,
            senderName = message.senderName,
            onLongPress = onLongPress,
            onSwipeReply = onSwipeReply,
            hapticFeedback = haptics
        )

        if (showReactionPicker) {
            Row(
                modifier = Modifier
                    .padding(start = if (message.isMine) 0.dp else 12.dp, top = 4.dp)
                    .align(if (message.isMine) Alignment.End else Alignment.Start),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                reactionPresets.forEach { emoji ->
                    Surface(
                        shape = RoundedCornerShape(dev.mizzenmast.letta.ui.components.LettaCorners.medium),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { onReact(emoji) },
                    ) {
                        Text(
                            text = emoji,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

// ─── MessageBubbleContent ────────────────────────────────────────────────────────────

@Composable
internal fun MessageBubbleContent(
    message: MessageEntity,
    replyMessage: MessageEntity?,
    onImageClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    linkPreviews: Map<String, LinkPreviewDto>,
    onRequestLinkPreview: (String) -> Unit,
) {
    val textColor = if (message.isMine) MaterialTheme.colorScheme.onPrimary
                   else MaterialTheme.colorScheme.onSurface
    val replyBackground = if (message.isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f)
                         else MaterialTheme.colorScheme.surfaceVariant
    val replyTextColor = if (message.isMine) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface
    val replyLabelColor = replyTextColor

    val context = LocalContext.current
    fun openMedia(url: String, mime: String?) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(url), mime ?: "*/*")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun openUrl(url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (replyMessage != null) {
            Surface(color = replyBackground, shape = RoundedCornerShape(dev.mizzenmast.letta.ui.components.LettaCorners.small + dev.mizzenmast.letta.ui.components.LettaSpacing.extraSmall / 2)) {
                Column(Modifier.padding(8.dp)) {
                    Text(
                        text = replyMessage.senderName.ifBlank { "Message" },
                        style = MaterialTheme.typography.labelSmall,
                        color = replyLabelColor,
                    )
                    Text(
                        text = replyMessage.content ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = replyTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        when (message.type) {
            "text" -> {
                Text(
                    text = message.content ?: "",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        letterSpacing = 0.01.sp
                    ),
                    color = textColor
                )
                        val candidate = remember(message.content) { extractFirstUrl(message.content) }
                        val normalized = remember(candidate) { candidate?.let { normalizeUrl(it) } }
                        if (candidate != null && normalized != null && shouldPreviewLink(candidate, message.content)) {
                            LaunchedEffect(normalized) { onRequestLinkPreview(normalized) }
                            val preview = linkPreviews[normalized]
                            if (preview != null) {
                                LinkPreviewCard(
                                    preview = preview,
                                    isMine = message.isMine,
                                    onClick = { openUrl(preview.url) },
                                )
                            }
                        }
                    }
                    "image" -> {
                        if (message.mediaUrl != null) {
                            AsyncImage(
                                model = message.mediaUrl,
                                contentDescription = "Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .clip(RoundedCornerShape(dev.mizzenmast.letta.ui.components.LettaCorners.medium))
                                    .clickable { onImageClick(message.mediaUrl) },
                            )
                        } else {
                            Text("Image", style = MaterialTheme.typography.bodyMedium, color = textColor)
                        }
                    }
                    "audio" -> {
                        if (message.mediaUrl != null) {
                            AudioPlayerBubble(
                                url = message.mediaUrl,
                                mimeType = message.mediaMime,
                                isMine = message.isMine,
                            )
                        } else {
                            Text("Audio", style = MaterialTheme.typography.bodyMedium, color = textColor)
                        }
                    }
                    "video" -> {
                        if (message.mediaUrl != null) {
                            VideoPlayerBubble(
                                url = message.mediaUrl,
                                onExpand = { onVideoClick(message.mediaUrl) },
                            )
                        } else {
                            Text("Video", style = MaterialTheme.typography.bodyMedium, color = textColor)
                        }
                    }
                    "document" -> {
                        if (message.mediaUrl != null) {
                            DocumentAttachment(
                                url = message.mediaUrl,
                                mimeType = message.mediaMime,
                                onOpen = { openMedia(message.mediaUrl, message.mediaMime) },
                            )
                        } else {
                            Text("Document", style = MaterialTheme.typography.bodyMedium, color = textColor)
                        }
                    }
                    else -> {
                        Text(
                            text = message.content ?: "",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 15.sp,
                                lineHeight = 21.sp,
                                letterSpacing = 0.01.sp
                            ),
                            color = textColor
                        )
                    }
                }

                val caption = message.content?.takeIf { it.isNotBlank() }
                if (message.type != "text" && !caption.isNullOrBlank()) {
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            lineHeight = 21.sp,
                            letterSpacing = 0.01.sp
                        ),
                        color = textColor
                    )
                }
            }
}

// ─── AudioPlayerBubble ────────────────────────────────────────────────────────

@Composable
internal fun AudioPlayerBubble(
    url: String,
    mimeType: String?,
    isMine: Boolean,
) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
        }
    }
    var isPlaying by remember(url) { mutableStateOf(false) }
    var durationMs by remember(url) { mutableStateOf(0L) }
    var positionMs by remember(url) { mutableStateOf(0L) }
    var sliderValue by remember(url) { mutableStateOf(0f) }
    var isSeeking by remember(url) { mutableStateOf(false) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) durationMs = max(player.duration, 0L)
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener); player.release() }
    }

    LaunchedEffect(player) {
        while (isActive) {
            if (player.playbackState == Player.STATE_READY) {
                positionMs = player.currentPosition
                durationMs = max(player.duration, 0L)
            }
            delay(250)
        }
    }

    val progress = if (durationMs > 0L) positionMs / durationMs.toFloat() else 0f
    val onSurfaceColor = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    LaunchedEffect(progress) {
        if (!isSeeking) sliderValue = progress.coerceIn(0f, 1f)
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = audioLabel(mimeType),
            style = MaterialTheme.typography.labelSmall,
            color = onSurfaceColor.copy(alpha = 0.7f),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (isPlaying) player.pause() else player.play() }) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = onSurfaceColor,
                )
            }
            Slider(
                value = sliderValue,
                onValueChange = { isSeeking = true; sliderValue = it },
                onValueChangeFinished = {
                    isSeeking = false
                    if (durationMs > 0L) player.seekTo((durationMs * sliderValue).toLong())
                },
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatPlaybackTime(positionMs),
                style = MaterialTheme.typography.labelSmall,
                color = onSurfaceColor.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

// ─── VideoPlayerBubble ────────────────────────────────────────────────────────

@Composable
internal fun VideoPlayerBubble(
    url: String,
    onExpand: () -> Unit,
) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
        }
    }

    DisposableEffect(player) { onDispose { player.release() } }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 220.dp)
            .clip(RoundedCornerShape(dev.mizzenmast.letta.ui.components.LettaCorners.medium)),
    ) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
                androidx.media3.ui.PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                }
            },
            modifier = Modifier.matchParentSize(),
        )
        IconButton(
            onClick = onExpand,
            modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
        ) {
            Icon(Icons.Rounded.OpenInFull, contentDescription = "Open video")
        }
    }
}

// ─── FullscreenVideoDialog ────────────────────────────────────────────────────

@Composable
internal fun FullscreenVideoDialog(
    url: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) { onDispose { player.release() } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f)),
        ) {
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { ctx ->
                    androidx.media3.ui.PlayerView(ctx).apply {
                        this.player = player
                        useController = true
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(dev.mizzenmast.letta.ui.components.LettaSpacing.large)
                    .clip(RoundedCornerShape(dev.mizzenmast.letta.ui.components.LettaCorners.large)),
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Close video")
            }
        }
    }
}

// ─── DocumentAttachment ───────────────────────────────────────────────────────

@Composable
internal fun DocumentAttachment(
    url: String,
    mimeType: String?,
    onOpen: () -> Unit,
) {
    val info = documentInfo(mimeType, url)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(dev.mizzenmast.letta.ui.components.LettaCorners.medium))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onOpen)
            .padding(dev.mizzenmast.letta.ui.components.LettaSpacing.small + dev.mizzenmast.letta.ui.components.LettaSpacing.extraSmall / 2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dev.mizzenmast.letta.ui.components.LettaSpacing.small + dev.mizzenmast.letta.ui.components.LettaSpacing.extraSmall / 2),
    ) {
        Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(dev.mizzenmast.letta.ui.components.LettaCorners.small)) {
            Icon(imageVector = info.icon, contentDescription = null, modifier = Modifier.padding(dev.mizzenmast.letta.ui.components.LettaSpacing.small))
        }
        Column(Modifier.weight(1f)) {
            Text(text = info.label, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = url,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ─── LinkPreviewCard ──────────────────────────────────────────────────────────

@Composable
internal fun LinkPreviewCard(
    preview: LinkPreviewDto,
    isMine: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val title = preview.title?.trim().orEmpty()
    val description = preview.description?.trim().orEmpty()
    val siteName = preview.siteName?.trim().orEmpty()

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(dev.mizzenmast.letta.ui.components.LettaCorners.medium),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(dev.mizzenmast.letta.ui.components.LettaSpacing.small + dev.mizzenmast.letta.ui.components.LettaSpacing.extraSmall / 2),
            horizontalArrangement = Arrangement.spacedBy(dev.mizzenmast.letta.ui.components.LettaSpacing.small + dev.mizzenmast.letta.ui.components.LettaSpacing.extraSmall / 2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!preview.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = preview.imageUrl,
                    contentDescription = preview.title ?: "Link preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(dev.mizzenmast.letta.ui.components.LettaSizes.buttonHeight).clip(RoundedCornerShape(dev.mizzenmast.letta.ui.components.LettaCorners.small)),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                if (title.isNotEmpty()) Text(text = title, style = MaterialTheme.typography.bodyMedium, color = textColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (description.isNotEmpty()) Text(text = description, style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.8f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (siteName.isNotEmpty()) Text(text = siteName, style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
