package dev.mizzenmast.letta.ui.conversations

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.mizzenmast.letta.core.motion.HapticFeedback
import dev.mizzenmast.letta.core.motion.rememberHapticFeedback
import dev.mizzenmast.letta.core.motion.horizontalSwipe
import dev.mizzenmast.letta.core.theme.LocalLettaColors
import dev.mizzenmast.letta.data.local.entity.ConversationEntity
import dev.mizzenmast.letta.ui.components.LettaAvatar
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

enum class ConversationFilter { All, Chats, Groups }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    onConversationClick: (String) -> Unit,
    onNewConversation: () -> Unit,
    // null = hide the settings icon (handled by bottom nav); non-null = show it
    onSettingsClick: (() -> Unit)? = null,
    filter: ConversationFilter = ConversationFilter.All,
    bottomBar: @Composable () -> Unit = {},
    viewModel: ConversationListViewModel = hiltViewModel(),
) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val currentUserId = viewModel.currentUserId
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchFocus = remember { FocusRequester() }

    val filtered = remember(conversations, filter, searchQuery) {
        val base = when (filter) {
            ConversationFilter.All    -> conversations
            ConversationFilter.Chats  -> conversations.filter { it.type == "direct" }
            ConversationFilter.Groups -> conversations.filter { it.type == "group" }
        }
        if (searchQuery.isBlank()) {
            base
        } else {
            base.filter {
                (it.name ?: "").contains(searchQuery, ignoreCase = true) ||
                (it.lastMessageContent ?: "").contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            if (showSearch) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = {
                            showSearch = false
                            searchQuery = ""
                        }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Close search")
                        }
                    },
                    title = {
                        LaunchedEffect(showSearch) {
                            if (showSearch) searchFocus.requestFocus()
                        }
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search conversations…") },
                            singleLine = true,
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Rounded.Close, "Clear")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(searchFocus),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                            ),
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            "Letta",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.5).sp,
                            ),
                        )
                    },
                    actions = {
                        IconButton(onClick = { showSearch = true }) {
                            Icon(Icons.Rounded.Search, contentDescription = "Search")
                        }
                        if (onSettingsClick != null) {
                            IconButton(onClick = onSettingsClick) {
                                Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewConversation,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "New conversation")
            }
        },
        bottomBar = bottomBar,
    ) { padding ->
        if (filtered.isEmpty()) {
            val title = when (filter) {
                ConversationFilter.Groups -> "No groups yet"
                ConversationFilter.Chats  -> "No chats yet"
                ConversationFilter.All    -> "No conversations yet"
            }
            val subtitle = when (filter) {
                ConversationFilter.Groups -> "Create a group to get started"
                else                      -> "Tap + to start one"
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Chat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onNewConversation) { Text("Start a conversation") }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                items(filtered, key = { it.id }) { conversation ->
                    ConversationItem(
                        conversation = conversation,
                        currentUserId = currentUserId,
                        onClick = { onConversationClick(conversation.id) },
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
}

@Composable
private fun ConversationItem(
    conversation: ConversationEntity,
    currentUserId: String,
    onClick: () -> Unit,
) {
    val lc = LocalLettaColors.current
    val hasUnread = conversation.unreadCount > 0
    val isMineLastMessage = currentUserId.isNotEmpty() &&
        conversation.lastMessageSenderId == currentUserId
    val haptics = rememberHapticFeedback()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalSwipe(
                onSwipeLeft = { 
                    haptics.heavyClick()
                    // TODO: Implement mute functionality  
                },
                onSwipeRight = { 
                    haptics.heavyClick()
                    // TODO: Implement archive functionality
                },
                hapticFeedback = haptics
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar with online status indicator
        Box {
            LettaAvatar(
                name = conversation.name ?: "?",
                imageUrl = conversation.avatarUrl,
                size = 56.dp,
            )

            // Online status indicator (for direct chats only)
            if (conversation.type == "direct") {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.BottomEnd)
                        .background(Color(0xFF10B981), CircleShape)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            // ── Name + timestamp row ─────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = conversation.name ?: "Direct message",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (isMineLastMessage && conversation.lastMessageAt != null) {
                        Icon(
                            imageVector = Icons.Rounded.Done,
                            contentDescription = null,
                            tint = lc.text3,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    if (conversation.lastMessageAt != null) {
                        Text(
                            text = formatTime(conversation.lastMessageAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hasUnread) lc.accent else lc.text3,
                            fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }

            Spacer(Modifier.height(2.dp))

            // ── Preview + unread badge row ───────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val previewText = conversation.lastMessageContent
                    ?: if (conversation.lastMessageAt != null) "Message" else "No messages yet"
                Text(
                    text = previewText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hasUnread) MaterialTheme.colorScheme.onSurface else lc.text2,
                    fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (hasUnread) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .height(20.dp)
                            .widthIn(min = 20.dp)
                            .background(lc.accent, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (conversation.unreadCount > 99) "99+"
                                   else conversation.unreadCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(isoString: String): String {
    return try {
        val parsed = OffsetDateTime.parse(isoString)
        val today = LocalDate.now(parsed.offset)
        if (parsed.toLocalDate() == today) {
            parsed.format(DateTimeFormatter.ofPattern("HH:mm"))
        } else {
            parsed.format(DateTimeFormatter.ofPattern("MMM d"))
        }
    } catch (_: Exception) {
        ""
    }
}