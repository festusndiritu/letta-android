package dev.mizzenmast.letta.ui.calls

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallMade
import androidx.compose.material.icons.rounded.CallMissed
import androidx.compose.material.icons.rounded.CallReceived
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.mizzenmast.letta.core.theme.LocalLettaColors
import dev.mizzenmast.letta.ui.components.LettaAvatar
import java.time.Duration
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsScreen(
    onCallClick: (String) -> Unit,
    onNewCall: () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    viewModel: CallsViewModel = hiltViewModel(),
) {
    val lc = LocalLettaColors.current
    val callItems by viewModel.callItems.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<CallItem?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    val filtered = remember(callItems, searchQuery) {
        if (searchQuery.isBlank()) callItems
        else callItems.filter { it.conversationName.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search calls…") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            ),
                        )
                    } else {
                        Text(
                            "Calls",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) searchQuery = ""
                    }) {
                        Icon(
                            if (isSearchActive) Icons.Rounded.Close else Icons.Rounded.Search,
                            contentDescription = if (isSearchActive) "Close search" else "Search",
                        )
                    }
                    if (callItems.isNotEmpty()) {
                        IconButton(onClick = { showClearConfirm = true }) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear call log")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewCall,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Rounded.Call, contentDescription = "New call")
            }
        },
        bottomBar = bottomBar,
    ) { padding ->
        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.Call,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (searchQuery.isNotBlank()) "No matching calls" else "No recent calls",
                        style = MaterialTheme.typography.bodyLarge,
                        color = lc.text2,
                    )
                    if (searchQuery.isBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Your call history will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = lc.text3,
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                items(filtered, key = { it.call.id }) { item ->
                    CallRow(
                        item = item,
                        onClick = { selectedItem = item },
                        onCallAgain = {
                            val convId = item.conversationId
                            if (convId != null) onCallClick(convId)
                        },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 80.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp,
                    )
                }
            }
        }
    }

    // ── Call Detail Sheet ──────────────────────────────────────────────────
    selectedItem?.let { item ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { selectedItem = null },
            sheetState = sheetState,
        ) {
            CallDetailSheet(
                item = item,
                onMessage = {
                    val convId = item.conversationId
                    if (convId != null) onCallClick(convId)
                    selectedItem = null
                },
                onCallAgain = {
                    val convId = item.conversationId
                    if (convId != null) onCallClick(convId)
                    selectedItem = null
                },
                onRemove = {
                    viewModel.deleteCall(item.call.id)
                    selectedItem = null
                },
            )
        }
    }

    // ── Clear Confirm Dialog ───────────────────────────────────────────────
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear call log?") },
            text = { Text("All call history will be removed from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearCalls()
                    showClearConfirm = false
                }) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

// ─── Call Row ────────────────────────────────────────────────────────────────

@Composable
private fun CallRow(
    item: CallItem,
    onClick: () -> Unit,
    onCallAgain: () -> Unit,
) {
    val lc = LocalLettaColors.current
    val directionIcon = when {
        item.isMissed   -> Icons.Rounded.CallMissed
        item.isIncoming -> Icons.Rounded.CallReceived
        else            -> Icons.Rounded.CallMade
    }
    val directionColor = when {
        item.isMissed   -> lc.destructive
        item.isIncoming -> lc.positive
        else            -> lc.text2
    }
    val directionLabel = when {
        item.isMissed   -> "Missed"
        item.isIncoming -> "Incoming"
        else            -> "Outgoing"
    }
    val typeLabel = if (item.call.type == "video") "video" else "voice"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LettaAvatar(
            name = item.conversationName,
            imageUrl = null,
            size = 52.dp,
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.conversationName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (item.isMissed) FontWeight.SemiBold else FontWeight.Normal,
                color = if (item.isMissed) lc.destructive
                        else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = directionIcon,
                    contentDescription = directionLabel,
                    tint = directionColor,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = buildString {
                        append("$directionLabel $typeLabel call")
                        val dur = callDuration(item.call.createdAt, item.call.endedAt)
                        if (dur != null) append("  ·  $dur")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = lc.text2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (item.call.createdAt != null) {
            Text(
                text = fmtCallTime(item.call.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = lc.text3,
            )
        }

        Spacer(Modifier.width(4.dp))

        // Call-again button
        IconButton(onClick = onCallAgain) {
            Icon(
                imageVector = if (item.call.type == "video") Icons.Rounded.Videocam else Icons.Rounded.Call,
                contentDescription = "Call again",
                tint = lc.accent,
            )
        }
    }
}

// ─── Call Detail Sheet Content ───────────────────────────────────────────────

@Composable
private fun CallDetailSheet(
    item: CallItem,
    onMessage: () -> Unit,
    onCallAgain: () -> Unit,
    onRemove: () -> Unit,
) {
    val lc = LocalLettaColors.current
    val directionLabel = when {
        item.isMissed   -> "Missed"
        item.isIncoming -> "Incoming"
        else            -> "Outgoing"
    }
    val typeLabel = if (item.call.type == "video") "Video" else "Voice"
    val durationStr = callDuration(item.call.createdAt, item.call.endedAt)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
    ) {
        // Avatar + Name header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 16.dp),
        ) {
            LettaAvatar(name = item.conversationName, imageUrl = null, size = 56.dp)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    item.conversationName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "$directionLabel $typeLabel call",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (item.isMissed) lc.destructive else lc.text2,
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(12.dp))

        // Call details
        if (item.call.createdAt != null) {
            DetailRow("When", fmtCallDateTime(item.call.createdAt))
        }
        if (durationStr != null) {
            DetailRow("Duration", durationStr)
        } else if (item.isMissed) {
            DetailRow("Duration", "—")
        }
        DetailRow("Type", "$typeLabel call")

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(8.dp))

        // Actions
        ListItem(
            headlineContent = { Text("Message") },
            leadingContent = {
                Icon(Icons.AutoMirrored.Rounded.Message,
                    contentDescription = null, tint = lc.accent)
            },
            modifier = Modifier
                .clip(RoundedCornerShape(dev.mizzenmast.letta.ui.components.LettaCorners.medium))
                .clickable(onClick = onMessage),
        )
        ListItem(
            headlineContent = {
                Text(if (item.call.type == "video") "Video call again" else "Call again")
            },
            leadingContent = {
                Icon(
                    if (item.call.type == "video") Icons.Rounded.Videocam else Icons.Rounded.Call,
                    contentDescription = null,
                    tint = lc.accent,
                )
            },
            modifier = Modifier
                .clip(RoundedCornerShape(dev.mizzenmast.letta.ui.components.LettaCorners.medium))
                .clickable(onClick = onCallAgain),
        )
        ListItem(
            headlineContent = { Text("Remove from log", color = lc.destructive) },
            leadingContent = {
                Icon(Icons.Rounded.Delete, contentDescription = null, tint = lc.destructive)
            },
            modifier = Modifier
                .clip(RoundedCornerShape(dev.mizzenmast.letta.ui.components.LettaCorners.medium))
                .clickable(onClick = onRemove),
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    val lc = LocalLettaColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = lc.text2)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun callDuration(createdAt: String?, endedAt: String?): String? {
    if (createdAt == null || endedAt == null) return null
    return try {
        val start = OffsetDateTime.parse(createdAt)
        val end   = OffsetDateTime.parse(endedAt)
        val secs  = Duration.between(start.toInstant(), end.toInstant()).seconds
        if (secs <= 0) return null
        val h = secs / 3600
        val m = (secs % 3600) / 60
        val s = secs % 60
        if (h > 0) "%d:%02d:%02d".format(h, m, s)
        else       "%d:%02d".format(m, s)
    } catch (_: Exception) { null }
}

private fun fmtCallTime(isoString: String): String {
    return try {
        val parsed = OffsetDateTime.parse(isoString)
        val today  = LocalDate.now(parsed.offset)
        if (parsed.toLocalDate() == today) {
            parsed.format(DateTimeFormatter.ofPattern("HH:mm"))
        } else {
            parsed.format(DateTimeFormatter.ofPattern("MMM d"))
        }
    } catch (_: Exception) { "" }
}

private fun fmtCallDateTime(isoString: String): String {
    return try {
        val parsed = OffsetDateTime.parse(isoString)
        parsed.format(DateTimeFormatter.ofPattern("MMM d, yyyy  HH:mm"))
    } catch (_: Exception) { isoString }
}


