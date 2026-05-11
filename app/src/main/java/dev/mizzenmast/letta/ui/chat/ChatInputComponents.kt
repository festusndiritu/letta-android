package dev.mizzenmast.letta.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.emoji2.emojipicker.EmojiPickerView
import dev.mizzenmast.letta.data.local.entity.ConversationMemberEntity
import dev.mizzenmast.letta.data.remote.dto.MessageDto
import dev.mizzenmast.letta.ui.components.rememberClampedKeyboardHeight

// ─── RecordingBar ─────────────────────────────────────────────────────────────

@Composable
internal fun RecordingBar(
    elapsedMs: Long,
    onCancel: () -> Unit,
    onSend: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(dev.mizzenmast.letta.ui.components.LettaCorners.huge),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dev.mizzenmast.letta.ui.components.LettaSpacing.medium,
                    vertical = dev.mizzenmast.letta.ui.components.LettaSpacing.small
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error),
                )
                Text(
                    text = "Recording ${formatDuration(elapsedMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Rounded.Close, contentDescription = "Cancel recording")
                }
                IconButton(onClick = onSend) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send recording")
                }
            }
        }
    }
}

// ─── AttachmentActionCard ─────────────────────────────────────────────────────

internal data class AttachmentItem(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
)

@Composable
internal fun AttachmentActionCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(dev.mizzenmast.letta.ui.components.LettaCorners.large),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = dev.mizzenmast.letta.ui.components.LettaSpacing.medium + 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(34.dp))
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─── MessageSearchSheet ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MessageSearchSheet(
    members: List<ConversationMemberEntity>,
    results: List<MessageDto>,
    isSearching: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSearch: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Search messages",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search") },
                placeholder = { Text("Type a keyword") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { onSearch(query) }) {
                        Icon(Icons.Rounded.Search, contentDescription = "Search")
                    }
                },
            )

            when {
                isSearching -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
                errorMessage != null -> {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                results.isEmpty() -> {
                    Text(
                        text = if (query.isBlank()) "Enter a keyword to search." else "No results.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                    ) {
                        items(results, key = { it.id }) { result ->
                            val senderName = members.firstOrNull { it.userId == result.senderId }?.displayName ?: "Unknown"
                            val preview = searchPreview(result)
                            val time = formatCreatedAt(result.createdAt)
                            ListItem(
                                headlineContent = { Text(preview, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                supportingContent = {
                                    Text(
                                        text = listOfNotNull(senderName, time).joinToString(" • "),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

// ─── EmojiPickerPanel ─────────────────────────────────────────────────────────

@Composable
internal fun EmojiPickerPanel(
    onEmojiSelected: (String) -> Unit,
    onBackspace: () -> Unit,
    onShowKeyboard: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var pickerView by remember { mutableStateOf<EmojiPickerView?>(null) }
    val keyboardHeight by rememberClampedKeyboardHeight(minHeight = 280.dp, maxHeight = 400.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(keyboardHeight)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Search bar on top - minimal, integrated look
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextField(
                    value = searchQuery,
                    onValueChange = { value ->
                        searchQuery = value
                        pickerView?.let { updateEmojiSearch(it, value) }
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            "Search emoji",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        disabledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    ),
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                IconButton(
                    onClick = onBackspace,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Backspace,
                        contentDescription = "Backspace",
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onShowKeyboard,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Rounded.Keyboard,
                        contentDescription = "Show keyboard",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Emoji grid - fills remaining space
        AndroidView(
            factory = { ctx ->
                EmojiPickerView(ctx).apply {
                    setOnEmojiPickedListener { item -> onEmojiSelected(item.emoji) }
                    pickerView = this
                    updateEmojiSearch(this, searchQuery)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}

internal fun updateEmojiSearch(view: EmojiPickerView, query: String) {
    val method = view.javaClass.methods.firstOrNull { method ->
        val name = method.name
        val paramTypes = method.parameterTypes
        val acceptsQuery = paramTypes.size == 1 &&
            (paramTypes[0] == String::class.java || CharSequence::class.java.isAssignableFrom(paramTypes[0]))
        acceptsQuery && name in setOf("setSearchQuery", "setSearchText", "setQuery", "setSearchTerm")
    }
    if (method != null) {
        runCatching { method.invoke(view, query) }
    }
}
