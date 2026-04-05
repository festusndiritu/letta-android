package dev.mizzenmast.letta.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------------------------------------------------------------------------
// Requests
// ---------------------------------------------------------------------------

@Serializable
data class RequestOtpRequest(
    @SerialName("phone_number") val phoneNumber: String,
)

@Serializable
data class VerifyOtpRequest(
    @SerialName("phone_number") val phoneNumber: String,
    @SerialName("code") val code: String,
    // display_name removed — collected in DisplayNameScreen, sent via CompleteProfileRequest
)

@Serializable
data class CompleteProfileRequest(
    @SerialName("setup_token") val setupToken: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
data class RefreshRequest(
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class UpdateProfileRequest(
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("bio") val bio: String? = null,
    @SerialName("presence_visible") val presenceVisible: Boolean? = null,
    @SerialName("receipts_visible") val receiptsVisible: Boolean? = null,
    @SerialName("show_timestamps") val showTimestamps: Boolean? = null,
)

@Serializable
data class PushTokenRequest(
    @SerialName("fcm_token") val fcmToken: String,
)

// ---------------------------------------------------------------------------
// Responses
// ---------------------------------------------------------------------------

@Serializable
data class RequestOtpResponse(
    @SerialName("message") val message: String,
)

/**
 * Response from /auth/verify-otp.
 *
 * Existing user → needsProfile=false, accessToken+refreshToken populated, setupToken=null
 * New user      → needsProfile=true,  setupToken populated, accessToken+refreshToken=null
 */
@Serializable
data class VerifyOtpResponse(
    @SerialName("needs_profile") val needsProfile: Boolean,
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("token_type") val tokenType: String? = null,
    @SerialName("setup_token") val setupToken: String? = null,
)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("token_type") val tokenType: String,
)

@Serializable
data class UploadAvatarResponse(
    @SerialName("url") val url: String,
)

@Serializable
data class UserDto(
    @SerialName("id") val id: String,
    @SerialName("phone_number") val phoneNumber: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("bio") val bio: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("presence_visible") val presenceVisible: Boolean,
    @SerialName("receipts_visible") val receiptsVisible: Boolean,
    @SerialName("show_timestamps") val showTimestamps: Boolean,
)