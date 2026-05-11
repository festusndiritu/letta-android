package dev.mizzenmast.letta.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Displays grouped images in a message bubble.
 * Supports 1-4 images with smart grid layout and proper accessibility.
 */
@Composable
fun GroupedImages(
    imageUrls: List<String>,
    onImageClick: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (imageUrls.size) {
        0 -> Unit
        1 -> {
            // Single image - full width
            SingleImage(
                url = imageUrls[0],
                onClick = { onImageClick(imageUrls[0], 0) },
                modifier = modifier,
                contentDescription = "Image"
            )
        }
        2 -> {
            // Two images - side by side
            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(dev.mizzenmast.letta.ui.components.LettaSpacing.extraSmall / 2)
            ) {
                imageUrls.forEachIndexed { index, url ->
                    AsyncImage(
                        model = url,
                        contentDescription = "Image ${index + 1} of ${imageUrls.size}",
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onImageClick(url, index) },
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        3 -> {
            // Three images - 1 large on left, 2 stacked on right
            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                AsyncImage(
                    model = imageUrls[0],
                    contentDescription = "Image 1",
                    modifier = Modifier
                        .weight(1f)
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onImageClick(imageUrls[0], 0) },
                    contentScale = ContentScale.Crop
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    imageUrls.drop(1).forEachIndexed { index, url ->
                        AsyncImage(
                            model = url,
                            contentDescription = "Image ${index + 2}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(99.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onImageClick(url, index + 1) },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
        else -> {
            // Four or more images - 2x2 grid (show first 4)
            Column(
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                imageUrls.take(4).chunked(2).forEach { rowImages ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        rowImages.forEachIndexed { index, url ->
                            val globalIndex = imageUrls.indexOf(url)
                            AsyncImage(
                                model = url,
                                contentDescription = "Image ${globalIndex + 1}",
                                modifier = Modifier
                                    .weight(1f)
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onImageClick(url, globalIndex) },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SingleImage(
    url: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Image",
) {
    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp)
            .clip(RoundedCornerShape(dev.mizzenmast.letta.ui.components.LettaCorners.medium))
            .clickable(
                onClick = onClick,
                onClickLabel = "View full size"
            ),
        contentScale = ContentScale.Crop
    )
}
