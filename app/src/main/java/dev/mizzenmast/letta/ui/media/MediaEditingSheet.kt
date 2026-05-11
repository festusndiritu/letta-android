package dev.mizzenmast.letta.ui.media

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import dev.mizzenmast.letta.core.motion.rememberHapticFeedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * Media editing sheet for images and videos before sending.
 * Features:
 * - Image cropping with drag/pinch gestures
 * - Multiple image selection and preview
 * - Caption input
 * - Send/cancel actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaEditingSheet(
    selectedUris: List<Uri>,
    onDismiss: () -> Unit,
    onSend: (List<Uri>, String?) -> Unit,
) {
    val haptics = rememberHapticFeedback()
    var caption by remember(selectedUris) { mutableStateOf("") }
    var currentIndex by remember(selectedUris) { mutableIntStateOf(0) }
    val currentUri = selectedUris.getOrNull(currentIndex)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = dev.mizzenmast.letta.ui.components.LettaSpacing.large)
        ) {
            // Header with count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dev.mizzenmast.letta.ui.components.LettaSpacing.large,
                        vertical = dev.mizzenmast.letta.ui.components.LettaSpacing.small
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { 
                    haptics.click()
                    onDismiss() 
                }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Cancel")
                }
                
                if (selectedUris.size > 1) {
                    Text(
                        text = "${currentIndex + 1} / ${selectedUris.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                TextButton(
                    onClick = {
                        haptics.click()
                        onSend(selectedUris, caption.takeIf { it.isNotBlank() })
                    }
                ) {
                    Text("Send", fontWeight = FontWeight.SemiBold)
                }
            }

            // Image preview/editor
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (currentUri != null) {
                    CroppableImageView(uri = currentUri)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Thumbnail carousel if multiple images
            if (selectedUris.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedUris.forEachIndexed { index, uri ->
                        ImageThumbnail(
                            uri = uri,
                            isSelected = index == currentIndex,
                            onClick = {
                                haptics.tick()
                                currentIndex = index
                            }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Editing tools
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                EditingTool(
                    icon = Icons.Rounded.Crop,
                    label = "Crop",
                    onClick = { haptics.click() }
                )
                EditingTool(
                    icon = Icons.Rounded.Tune,
                    label = "Filters",
                    onClick = { haptics.click() }
                )
                EditingTool(
                    icon = Icons.Rounded.TextFields,
                    label = "Text",
                    onClick = { haptics.click() }
                )
                EditingTool(
                    icon = Icons.Rounded.Draw,
                    label = "Draw",
                    onClick = { haptics.click() }
                )
            }

            Spacer(Modifier.height(16.dp))

            // Caption input
            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Add a caption...") },
                shape = RoundedCornerShape(24.dp),
                maxLines = 3
            )
        }
    }
}

@Composable
private fun CroppableImageView(uri: Uri) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current)
            .data(uri)
            .crossfade(true)
            .build()
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 4f)
                    offset += pan
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painter,
            contentDescription = "Selected media",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun ImageThumbnail(
    uri: Uri,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else Modifier
            )
    ) {
        Image(
            painter = rememberAsyncImagePainter(uri),
            contentDescription = "Thumbnail",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun EditingTool(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
