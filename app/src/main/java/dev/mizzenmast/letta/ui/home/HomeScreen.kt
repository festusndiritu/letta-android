package dev.mizzenmast.letta.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import dev.mizzenmast.letta.ui.conversations.ConversationFilter
import dev.mizzenmast.letta.ui.conversations.ConversationListScreen

private enum class HomeTab(val label: String) {
    Chats("Chats"),
    Groups("Groups"),
}

@Composable
fun HomeScreen(
    onConversationClick: (String) -> Unit,
    onNewConversation: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(HomeTab.Chats) }

    ConversationListScreen(
        filter = if (selectedTab == HomeTab.Chats) ConversationFilter.Chats else ConversationFilter.Groups,
        onConversationClick = onConversationClick,
        onNewConversation = onNewConversation,
        onSettingsClick = onSettingsClick,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == HomeTab.Chats,
                    onClick = { selectedTab = HomeTab.Chats },
                    icon = { Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = "Chats") },
                    label = { Text(HomeTab.Chats.label) },
                )
                NavigationBarItem(
                    selected = selectedTab == HomeTab.Groups,
                    onClick = { selectedTab = HomeTab.Groups },
                    icon = { Icon(Icons.Rounded.Group, contentDescription = "Groups") },
                    label = { Text(HomeTab.Groups.label) },
                )
            }
        },
    )
}

