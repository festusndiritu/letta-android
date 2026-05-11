package dev.mizzenmast.letta.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timelapse
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import dev.mizzenmast.letta.ui.calls.CallsScreen
import dev.mizzenmast.letta.ui.conversations.ConversationFilter
import dev.mizzenmast.letta.ui.conversations.ConversationListScreen
import dev.mizzenmast.letta.ui.status.StatusScreen

private enum class HomeTab(val label: String) {
    Chats("Chats"),
    Status("Status"),
    Calls("Calls"),
    Settings("Settings"),
}

private fun iconFor(tab: HomeTab): ImageVector = when (tab) {
    HomeTab.Chats    -> Icons.AutoMirrored.Rounded.Chat
    HomeTab.Status   -> Icons.Rounded.Timelapse
    HomeTab.Calls    -> Icons.Rounded.Call
    HomeTab.Settings -> Icons.Rounded.Settings
}

@Composable
fun HomeScreen(
    onConversationClick: (String) -> Unit,
    onNewConversation: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(HomeTab.Chats) }

    val bottomBar: @Composable () -> Unit = {
        NavigationBar {
            HomeTab.entries.forEach { tab ->
                NavigationBarItem(
                    selected = selectedTab == tab && tab != HomeTab.Settings,
                    onClick = {
                        if (tab == HomeTab.Settings) {
                            onSettingsClick()
                        } else {
                            selectedTab = tab
                        }
                    },
                    icon = { Icon(iconFor(tab), contentDescription = tab.label) },
                    label = { Text(tab.label) },
                )
            }
        }
    }

    when (selectedTab) {
        HomeTab.Chats  -> ConversationListScreen(
            filter = ConversationFilter.All,
            onConversationClick = onConversationClick,
            onNewConversation = onNewConversation,
            bottomBar = bottomBar,
        )
        HomeTab.Status -> StatusScreen(
            bottomBar = bottomBar,
        )
        HomeTab.Calls  -> CallsScreen(
            onCallClick = onConversationClick,
            onNewCall = onNewConversation,
            bottomBar = bottomBar,
        )
        HomeTab.Settings -> Unit // handled via onSettingsClick above
    }
}

