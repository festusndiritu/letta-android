package dev.mizzenmast.letta.ui.media

import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage

/**
 * Represents a single media item in the full-screen viewer.
 * [mimeType] is optional; viewer infers image/video from "video/" prefix.
 */
data class MediaViewerItem(
    val url: String,
    val mimeType: String? = null,
    val caption: String? = null,
)

/**
 * Full-screen media viewer with:
 *  - Horizontal pager swipe between [items]
 *  - Pinch-to-zoom for images
 *  - Video playback (ExoPlayer) per page
 *  - Tap to toggle chrome (close / download buttons)
 *  - Caption overlay
 *
 * @param items         List of media items to show
 * @param initialIndex  The index to open first
 * @param onDismiss     Called when user closes the viewer
 * @param onDownload    Optional download action. If null, download button is hidden.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullscreenMediaViewer(
    items: List<MediaViewerItem>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit,
    onDownload: ((MediaViewerItem) -> Unit)? = null,
) {
    if (items.isEmpty()) return

    val pagerState = rememberPagerState(initialPage = initialIndex) { items.size }
    var chromeVisible by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val item = items[page]
            val isVideo = item.mimeType?.startsWith("video") == true ||
                item.url.contains(".mp4", ignoreCase = true) ||
                item.url.contains(".mov", ignoreCase = true) ||
                item.url.contains(".webm", ignoreCase = true)

            if (isVideo) {
                VideoPage(
                    url = item.url,
                    isActive = pagerState.currentPage == page,
                    onTap = { chromeVisible = !chromeVisible },
                )
            } else {
                ImagePage(
                    url = item.url,
                    onTap = { chromeVisible = !chromeVisible },
                )
            }
        }

        // ── Chrome overlay ────────────────────────────────────────────────
        val chromeAlpha by animateFloatAsState(
            targetValue = if (chromeVisible) 1f else 0f,
            animationSpec = tween(200),
            label = "chrome_alpha",
        )

        if (chromeAlpha > 0f) {
            // Top bar
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = chromeAlpha },
            ) {
                // Close button
                Box(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(8.dp)
                        .align(Alignment.TopStart),
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f)),
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // Page counter
                Text(
                    text = "${pagerState.currentPage + 1} / ${items.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(top = 14.dp)
                        .align(Alignment.TopCenter),
                )

                // Download button
                if (onDownload != null) {
                    Box(
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(8.dp)
                            .align(Alignment.TopEnd),
                    ) {
                        IconButton(
                            onClick = { onDownload(items[pagerState.currentPage]) },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f)),
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = "Download", tint = Color.White)
                        }
                    }
                }

                // Caption
                val currentItem = items[pagerState.currentPage]
                if (!currentItem.caption.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .padding(bottom = 16.dp),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                        ) {
                            Text(
                                text = currentItem.caption,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Image page with pinch-to-zoom ─────────────────────────────────────────────

@Composable
private fun ImagePage(
    url: String,
    onTap: () -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onTap() }, onDoubleTap = {
                    // Double-tap toggles zoom 1x ↔ 2.5x
                    scale = if (scale > 1.5f) 1f else 2.5f
                    if (scale == 1f) { offsetX = 0f; offsetY = 0f }
                })
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    if (scale > 1f) {
                        offsetX += pan.x
                        offsetY += pan.y
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY,
                ),
        )
    }
}

// ── Video page ────────────────────────────────────────────────────────────────

@Composable
private fun VideoPage(
    url: String,
    isActive: Boolean,
    onTap: () -> Unit,
) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
        }
    }

    DisposableEffect(url) { onDispose { player.release() } }

    // Pause when not the active page
    DisposableEffect(isActive) {
        if (isActive) player.play() else player.pause()
        onDispose {}
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = true
                setOnClickListener { onTap() }
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}
