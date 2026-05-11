package dev.mizzenmast.letta.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.mizzenmast.letta.data.local.TokenStore
import dev.mizzenmast.letta.service.CallStateManager
import dev.mizzenmast.letta.ui.SplashScreen
import dev.mizzenmast.letta.ui.auth.AuthEvent
import dev.mizzenmast.letta.ui.auth.AuthViewModel
import dev.mizzenmast.letta.ui.auth.DisplayNameScreen
import dev.mizzenmast.letta.ui.auth.OtpScreen
import dev.mizzenmast.letta.ui.auth.PhoneEntryScreen
import dev.mizzenmast.letta.ui.chat.ChatScreen
import dev.mizzenmast.letta.ui.conversations.NewConversationScreen
import dev.mizzenmast.letta.ui.groups.AddGroupMembersScreen
import dev.mizzenmast.letta.ui.groups.CreateGroupScreen
import dev.mizzenmast.letta.ui.groups.GroupInfoScreen
import dev.mizzenmast.letta.ui.groups.GroupSettingsScreen
import dev.mizzenmast.letta.ui.home.HomeScreen
import dev.mizzenmast.letta.ui.profile.ProfileScreen
import dev.mizzenmast.letta.ui.settings.FocusProfileScreen
import dev.mizzenmast.letta.ui.settings.ProfileSettingsScreen
import dev.mizzenmast.letta.ui.settings.SessionsScreen
import dev.mizzenmast.letta.ui.settings.SettingsScreen
import dev.mizzenmast.letta.ui.settings.ThemeSettingsScreen

@Composable
fun LettaNavGraph(
    navController: NavHostController,
    tokenStore: TokenStore,
    startChatConversationId: String? = null,
    callStateManager: CallStateManager? = null,
) {
    NavHost(
        navController = navController,
        startDestination = Route.Splash.path,
    ) {
        // Splash
        composable(Route.Splash.path) {
            SplashScreen(
                onComplete = {
                    val dest = if (tokenStore.isLoggedIn())
                        Route.ConversationList.path
                    else
                        Route.PhoneEntry.path
                    navController.navigate(dest) {
                        popUpTo(Route.Splash.path) { inclusive = true }
                    }
                }
            )
        }

        // Auth
        composable(Route.PhoneEntry.path) {
            val authViewModel: AuthViewModel = hiltViewModel()
            val uiState = authViewModel.uiState.collectAsStateWithLifecycle().value
            val event = authViewModel.events.collectAsStateWithLifecycle().value

            androidx.compose.runtime.LaunchedEffect(event) {
                when (event) {
                    is AuthEvent.OtpSent -> {
                        navController.navigate(Route.Otp.createRoute(uiState.phoneNumber))
                        authViewModel.consumeEvent()
                    }
                    else -> Unit
                }
            }

            PhoneEntryScreen(uiState = uiState, onRequestOtp = { authViewModel.requestOtp(it) })
        }

        composable(
            route = Route.Otp.path,
            arguments = listOf(navArgument("phoneNumber") { type = NavType.StringType }),
        ) { back ->
            val phoneNumber = back.arguments?.getString("phoneNumber") ?: ""
            val parent = remember(back) { navController.getBackStackEntry(Route.PhoneEntry.path) }
            val authViewModel: AuthViewModel = hiltViewModel(parent)
            val uiState = authViewModel.uiState.collectAsStateWithLifecycle().value
            val event = authViewModel.events.collectAsStateWithLifecycle().value

            LaunchedEffect(event) {
                when (event) {
                    is AuthEvent.Verified -> {
                        navController.navigate(Route.ConversationList.path) {
                            popUpTo(Route.PhoneEntry.path) { inclusive = true }
                        }
                        authViewModel.consumeEvent()
                    }
                    is AuthEvent.NewUser -> {
                        navController.navigate(Route.DisplayName.createRoute(phoneNumber))
                        authViewModel.consumeEvent()
                    }
                    else -> Unit
                }
            }

            OtpScreen(
                phoneNumber = phoneNumber,
                uiState = uiState,
                onVerifyOtp = { authViewModel.verifyOtp(it) },
                onResendOtp = {authViewModel.resendOtp()},
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Route.DisplayName.path,
            arguments = listOf(navArgument("phoneNumber") { type = NavType.StringType }),
        ) { back ->
            val parent = remember(back) { navController.getBackStackEntry(Route.PhoneEntry.path) }
            val authViewModel: AuthViewModel = hiltViewModel(parent)
            val uiState = authViewModel.uiState.collectAsStateWithLifecycle().value
            val event = authViewModel.events.collectAsStateWithLifecycle().value

            LaunchedEffect(event) {
                when (event) {
                    is AuthEvent.Complete -> {
                        navController.navigate(Route.ConversationList.path) {
                            popUpTo(Route.PhoneEntry.path) { inclusive = true }
                        }
                        authViewModel.consumeEvent()
                    }
                    else -> Unit
                }
            }

            DisplayNameScreen(uiState = uiState, onSetDisplayName = { authViewModel.completeProfile(it) })
        }

        // Main
        composable(Route.ConversationList.path) {
            var handledStartChat by remember { mutableStateOf(false) }

            LaunchedEffect(startChatConversationId) {
                if (!handledStartChat && !startChatConversationId.isNullOrBlank()) {
                    handledStartChat = true
                    navController.navigate(Route.Chat.createRoute(startChatConversationId))
                }
            }

            HomeScreen(
                onConversationClick = { navController.navigate(Route.Chat.createRoute(it)) },
                onNewConversation = { navController.navigate(Route.NewConversation.path) },
                onSettingsClick = { navController.navigate(Route.Settings.path) },
            )
        }

        composable(
            route = Route.Chat.path,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType }),
        ) { back ->
            ChatScreen(
                conversationId = back.arguments?.getString("conversationId") ?: "",
                onBack = { navController.popBackStack() },
                onGroupInfoClick = {
                    navController.navigate(Route.GroupSettings.createRoute(back.arguments?.getString("conversationId") ?: ""))
                },
                onProfileClick = { navController.navigate(Route.Profile.createRoute(it)) },
            )
        }

        composable(Route.NewConversation.path) {
            NewConversationScreen(
                onConversationCreated = {
                    navController.navigate(Route.Chat.createRoute(it)) {
                        popUpTo(Route.NewConversation.path) { inclusive = true }
                    }
                },
                onCreateGroup = { navController.navigate(Route.CreateGroup.path) },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Route.CreateGroup.path) {
            CreateGroupScreen(
                onGroupCreated = {
                    navController.navigate(Route.Chat.createRoute(it)) {
                        popUpTo(Route.NewConversation.path) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Route.GroupInfo.path,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType }),
        ) { back ->
            GroupInfoScreen(
                conversationId = back.arguments?.getString("conversationId") ?: "",
                onBack = { navController.popBackStack() },
                onMemberClick = { navController.navigate(Route.Profile.createRoute(it)) },
            )
        }

        composable(
            route = Route.GroupSettings.path,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType }),
        ) { back ->
            val conversationId = back.arguments?.getString("conversationId") ?: ""
            GroupSettingsScreen(
                conversationId = conversationId,
                onBack = { navController.popBackStack() },
                onAddMembers = { navController.navigate(Route.AddGroupMembers.createRoute(it)) },
                onMemberClick = { navController.navigate(Route.Profile.createRoute(it)) },
            )
        }

        composable(
            route = Route.AddGroupMembers.path,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType }),
        ) { back ->
            val conversationId = back.arguments?.getString("conversationId") ?: ""
            AddGroupMembersScreen(
                conversationId = conversationId,
                onBack = { navController.popBackStack() },
                onAdded = { navController.popBackStack() },
            )
        }

        composable(
            route = Route.Profile.path,
            arguments = listOf(navArgument("userId") { type = NavType.StringType }),
        ) { back ->
            ProfileScreen(
                userId = back.arguments?.getString("userId") ?: "",
                onBack = { navController.popBackStack() },
                onStartChat = {
                    navController.navigate(Route.Chat.createRoute(it)) {
                        popUpTo(Route.Profile.path) { inclusive = true }
                    }
                },
            )
        }

        composable(Route.Settings.path) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onFocusProfileClick = { navController.navigate(Route.FocusProfile.path) },
                onSessionsClick = { navController.navigate(Route.Sessions.path) },
                onThemeClick = { navController.navigate(Route.ThemeSettings.path) },
                onProfileClick = { navController.navigate(Route.ProfileSettings.path) },
            )
        }

        composable(Route.ProfileSettings.path) {
            ProfileSettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Route.FocusProfile.path) {
            FocusProfileScreen(onBack = { navController.popBackStack() })
        }

        composable(Route.Sessions.path) {
            SessionsScreen(onBack = { navController.popBackStack() })
        }

        composable(Route.ThemeSettings.path) {
            ThemeSettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}