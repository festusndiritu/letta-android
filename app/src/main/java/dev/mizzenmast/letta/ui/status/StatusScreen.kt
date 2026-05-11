package dev.mizzenmast.letta.ui.status

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import dev.mizzenmast.letta.core.theme.LocalLettaColors
import dev.mizzenmast.letta.data.local.entity.StatusEntity
import dev.mizzenmast.letta.ui.components.LettaAvatar
import dev.mizzenmast.letta.ui.media.FullscreenMediaViewer
import dev.mizzenmast.letta.ui.media.MediaViewerItem
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusScreen(
    bottomBar: @Composable () -> Unit = {},
    viewModel: StatusViewModel = hiltViewModel(),
) {
    val lc = LocalLettaColors.current
    val myStatuses by viewModel.myStatuses.collectAsStateWithLifecycle()
    val feedGroups by viewModel.feedGroups.collectAsStateWithLifecycle()

    // State for fullscreen story viewer
    var viewerGroup by remember { mutableStateOf<StatusGroup?>(null) }
    var viewerStartIndex by remember { mutableIntStateOf(0) }
    var viewingMine by remember { mutableStateOf(false) }
    var showComposer by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Status",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showComposer = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Rounded.CameraAlt, contentDescription = "Add status")
            }
        },
        bottomBar = bottomBar,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // ── My status ─────────────────────────────────────────────────────
            item {
                Text(
                    text = "My status",
                    style = MaterialTheme.typography.labelMedium,
                    color = lc.text3,
                    modifier = Modifier.padding(
                        horizontal = dev.mizzenmast.letta.ui.components.LettaSpacing.large,
                        vertical = dev.mizzenmast.letta.ui.components.LettaSpacing.small
                    ),
                )
            }
            item {
                MyStatusRow(
                    statuses = myStatuses,
                    onTap = {
                        if (myStatuses.isNotEmpty()) viewingMine = true
                        else showComposer = true
                    },
                    onDelete = { statusId -> viewModel.deleteStatus(statusId) },
                )
            }

            // ── Recent updates ────────────────────────────────────────────────
            if (feedGroups.isNotEmpty()) {
                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = dev.mizzenmast.letta.ui.components.LettaSpacing.extraSmall),
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp,
                    )
                    Text(
                        text = "Recent updates",
                        style = MaterialTheme.typography.labelMedium,
                        color = lc.text3,
                        modifier = Modifier.padding(
                            horizontal = dev.mizzenmast.letta.ui.components.LettaSpacing.large,
                            vertical = dev.mizzenmast.letta.ui.components.LettaSpacing.small
                        ),
                    )
                }
                items(feedGroups, key = { it.userId }) { group ->
                    StatusContactRow(
                        group = group,
                        onTap = {
                            viewerGroup = group
                            viewerStartIndex = 0
                        },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 80.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp,
                    )
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "No recent updates from contacts.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = lc.text3,
                        )
                    }
                }
            }
        }
    }

    // ── Fullscreen story viewer for own statuses ──────────────────────────
    if (viewingMine && myStatuses.isNotEmpty()) {
        val items = myStatuses.map { s ->
            MediaViewerItem(
                url = s.mediaUrl ?: "",
                mimeType = if (s.type == "video") "video/mp4" else "image/jpeg",
                caption = s.content,
            )
        }.filter { it.url.isNotEmpty() }
        if (items.isNotEmpty()) {
            FullscreenMediaViewer(
                items = items,
                initialIndex = 0,
                onDismiss = { viewingMine = false },
            )
        } else {
            LaunchedEffect(Unit) { viewingMine = false }
        }
    }

    // ── Fullscreen story viewer for contact statuses ───────────────────────
    val activeGroup = viewerGroup
    if (activeGroup != null) {
        val items = activeGroup.statuses.map { s ->
            MediaViewerItem(
                url = s.mediaUrl ?: "",
                mimeType = if (s.type == "video") "video/mp4" else "image/jpeg",
                caption = s.content,
            )
        }.filter { it.url.isNotEmpty() }
        if (items.isNotEmpty()) {
            FullscreenMediaViewer(
                items = items,
                initialIndex = viewerStartIndex.coerceAtMost(items.lastIndex),
                onDismiss = {
                    viewerGroup = null
                    // mark viewed
                    activeGroup.statuses.forEach { s ->
                        viewModel.viewStatus(s.id)
                    }
                },
            )
        } else {
            // Text-only statuses — dismiss immediately (no viewer needed for text)
            LaunchedEffect(activeGroup) { viewerGroup = null }
        }
    }

    // ── Status composer sheet ────────────────────────────────────────────────
    if (showComposer) {
        StatusComposerSheet(
            onDismiss = { showComposer = false },
            onPostText = { content, bgColor ->
                viewModel.createTextStatus(content, bgColor)
                showComposer = false
            },
            onPostMedia = { type, mediaUrl ->
                viewModel.createMediaStatus(type, mediaUrl, null)
                showComposer = false
            },
        )
    }
}

// ── My status card ─────────────────────────────────────────────────────────────

@Composable
private fun MyStatusRow(
    statuses: List<StatusEntity>,
    onTap: () -> Unit,
    onDelete: (String) -> Unit = {},
) {
    val lc = LocalLettaColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            StoryRingAvatar(
                name = "Me",
                avatarUrl = null,
                hasUnviewed = statuses.isNotEmpty(),
                size = 52.dp,
            )
            if (statuses.isEmpty()) {
                // Show a "+" overlay when no statuses yet
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                "My status",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = if (statuses.isEmpty()) "Tap to add your first update"
                       else "${statuses.size} update${if (statuses.size != 1) "s" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = lc.text2,
            )
        }
    }
}

// ── Contact status row ─────────────────────────────────────────────────────────

@Composable
private fun StatusContactRow(
    group: StatusGroup,
    onTap: () -> Unit,
) {
    val lc = LocalLettaColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StoryRingAvatar(
            name = group.displayName,
            avatarUrl = group.avatarUrl,
            hasUnviewed = true,
            size = 52.dp,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                group.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val latest = group.statuses.lastOrNull()
            Text(
                text = buildString {
                    append("${group.statuses.size} update${if (group.statuses.size != 1) "s" else ""}")
                    if (latest?.createdAt != null) {
                        append("  ·  ")
                        append(fmtStatusTime(latest.createdAt))
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = lc.text2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ── Story ring avatar ──────────────────────────────────────────────────────────

@Composable
private fun StoryRingAvatar(
    name: String,
    avatarUrl: String?,
    hasUnviewed: Boolean,
    size: Dp,
) {
    val lc = LocalLettaColors.current
    val ringBrush = if (hasUnviewed) {
        Brush.sweepGradient(listOf(lc.accent, lc.accentLight, lc.accentMid, lc.accent))
    } else {
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.outline,
                MaterialTheme.colorScheme.outline,
            )
        )
    }
    Box(
        modifier = Modifier
            .size(size)
            .border(width = 2.dp, brush = ringBrush, shape = CircleShape)
            .padding(3.dp),
        contentAlignment = Alignment.Center,
    ) {
        LettaAvatar(
            name = name,
            imageUrl = avatarUrl,
            size = size - 6.dp,
        )
    }
}

private fun fmtStatusTime(isoString: String): String {
    return try {
        OffsetDateTime.parse(isoString)
            .format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (_: Exception) { "" }
}

// ── Status composer bottom sheet ───────────────────────────────────────────────

private val statusBgColors = listOf(
    "#1a1a2e", "#16213e", "#0f3460", "#533483",
    "#2d6a4f", "#1b4332", "#6b3fa0", "#b5179e",
    "#e63946", "#f77f00", "#2196f3", "#00b4d8",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusComposerSheet(
    onDismiss: () -> Unit,
    onPostText: (content: String, bgColor: String?) -> Unit,
    onPostMedia: (type: String, mediaUrl: String) -> Unit,
) {
    var tab by remember { mutableStateOf(0) } // 0=Text, 1=Photo, 2=Video
    var textContent by remember { mutableStateOf("") }
    var selectedBg by remember { mutableStateOf(statusBgColors.first()) }
    var pickedMediaUri by remember { mutableStateOf<Uri?>(null) }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) pickedMediaUri = uri
    }
    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) pickedMediaUri = uri
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                "New status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            // Tab selector
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("Text", "Photo", "Video").forEachIndexed { idx, label ->
                    SegmentedButton(
                        selected = tab == idx,
                        onClick = {
                            tab = idx
                            pickedMediaUri = null
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = 3),
                        label = { Text(label) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            when (tab) {
                0 -> {
                    // Text status
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(android.graphics.Color.parseColor(selectedBg)))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (textContent.isEmpty()) {
                            Text(
                                "What's on your mind?",
                                color = Color.White.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                            )
                        }
                        BasicTextField(
                            value = textContent,
                            onValueChange = { if (it.length <= 200) textContent = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.White,
                                textAlign = TextAlign.Center,
                            ),
                            decorationBox = { inner -> inner() },
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    // Background color picker
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        statusBgColors.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .border(
                                        width = if (selectedBg == hex) 2.5.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape,
                                    )
                                    .clickable { selectedBg = hex },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (selectedBg == hex) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Photo picker
                    if (pickedMediaUri != null) {
                        AsyncImage(
                            model = pickedMediaUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(dev.mizzenmast.letta.ui.components.LettaCorners.medium)),
                        )
                        Spacer(Modifier.height(dev.mizzenmast.letta.ui.components.LettaSpacing.small))
                        TextButton(onClick = { pickedMediaUri = null }) { Text("Remove") }
                    } else {
                        OutlinedButton(
                            onClick = { photoLauncher.launch("image/*") },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Rounded.Image, contentDescription = null)
                            Spacer(Modifier.width(dev.mizzenmast.letta.ui.components.LettaSpacing.small))
                            Text("Choose photo")
                        }
                    }
                }
                2 -> {
                    // Video picker
                    if (pickedMediaUri != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(dev.mizzenmast.letta.ui.components.LettaCorners.medium))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(dev.mizzenmast.letta.ui.components.LettaSpacing.large),
                        ) {
                            Icon(Icons.Rounded.VideoLibrary, contentDescription = null)
                            Spacer(Modifier.width(dev.mizzenmast.letta.ui.components.LettaSpacing.medium))
                            Text(
                                "Video selected",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            IconButton(onClick = { pickedMediaUri = null }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Remove")
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = { videoLauncher.launch("video/*") },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Rounded.VideoLibrary, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Choose video")
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            val canPost = when (tab) {
                0 -> textContent.isNotBlank()
                else -> pickedMediaUri != null
            }

            Button(
                onClick = {
                    when (tab) {
                        0 -> onPostText(textContent, selectedBg)
                        1 -> onPostMedia("image", pickedMediaUri.toString())
                        2 -> onPostMedia("video", pickedMediaUri.toString())
                    }
                },
                enabled = canPost,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Post status")
            }
        }
    }
}
