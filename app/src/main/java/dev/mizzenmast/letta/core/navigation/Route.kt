package dev.mizzenmast.letta.core.navigation

sealed class Route(val path: String) {
    data object Splash : Route("splash")
    data object PhoneEntry : Route("phone_entry")
    data object Otp : Route("otp/{phoneNumber}") {
        fun createRoute(phoneNumber: String) = "otp/$phoneNumber"
    }
    data object DisplayName : Route("display_name/{phoneNumber}") {
        fun createRoute(phoneNumber: String) = "display_name/$phoneNumber"
    }
    data object ConversationList : Route("conversation_list")
    data object Chat : Route("chat/{conversationId}") {
        fun createRoute(conversationId: String) = "chat/$conversationId"
    }
    data object NewConversation : Route("new_conversation")
    data object CreateGroup : Route("create_group")
    data object GroupInfo : Route("group_info/{conversationId}") {
        fun createRoute(conversationId: String) = "group_info/$conversationId"
    }
    data object GroupSettings : Route("group_settings/{conversationId}") {
        fun createRoute(conversationId: String) = "group_settings/$conversationId"
    }
    data object AddGroupMembers : Route("group_members/{conversationId}") {
        fun createRoute(conversationId: String) = "group_members/$conversationId"
    }
    data object Profile : Route("profile/{userId}") {
        fun createRoute(userId: String) = "profile/$userId"
    }
    data object Settings : Route("settings")
    data object ProfileSettings : Route("profile_settings")
    data object FocusProfile : Route("focus_profile")
    data object Sessions : Route("sessions")
    data object ThemeSettings : Route("theme_settings")
}