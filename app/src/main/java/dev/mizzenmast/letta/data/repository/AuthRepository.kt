package dev.mizzenmast.letta.data.repository

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.mizzenmast.letta.data.local.TokenStore
import dev.mizzenmast.letta.data.remote.api.AuthApiService
import dev.mizzenmast.letta.data.remote.dto.CompleteProfileRequest
import dev.mizzenmast.letta.data.remote.dto.PushTokenRequest
import dev.mizzenmast.letta.data.remote.dto.RequestOtpRequest
import dev.mizzenmast.letta.data.remote.dto.UpdateProfileRequest
import dev.mizzenmast.letta.data.remote.dto.UserDto
import dev.mizzenmast.letta.data.remote.dto.VerifyOtpRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthResult {
    data class ExistingUser(val user: UserDto) : AuthResult()
    // setupToken is passed straight through to completeProfile — never stored on disk
    data class NewUser(val setupToken: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: AuthApiService,
    private val tokenStore: TokenStore,
) {
    fun isLoggedIn(): Boolean = tokenStore.isLoggedIn()

    suspend fun requestOtp(phoneNumber: String): Result<Unit> {
        return try {
            api.requestOtp(RequestOtpRequest(phoneNumber))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyOtp(phoneNumber: String, code: String): AuthResult {
        return try {
            val response = api.verifyOtp(VerifyOtpRequest(phoneNumber = phoneNumber, code = code))

            if (response.needsProfile) {
                // New user — hand the setup token up to the ViewModel; don't touch TokenStore yet
                AuthResult.NewUser(setupToken = requireNotNull(response.setupToken) {
                    "Server returned needs_profile=true but no setup_token"
                })
            } else {
                // Existing user — save tokens and fetch profile
                tokenStore.accessToken = requireNotNull(response.accessToken)
                tokenStore.refreshToken = requireNotNull(response.refreshToken)
                val user = api.getMe()
                tokenStore.userId = user.id
                AuthResult.ExistingUser(user)
            }
        } catch (e: retrofit2.HttpException) {
            val message = when (e.code()) {
                400 -> "Invalid or expired code."
                429 -> "Too many attempts. Please wait before trying again."
                else -> "Something went wrong. Please try again."
            }
            AuthResult.Error(message)
        } catch (e: Exception) {
            AuthResult.Error("No internet connection.")
        }
    }

    /**
     * Called after DisplayNameScreen collects the user's name and optional avatar.
     * Sends to /auth/complete-profile, saves the returned token pair.
     */
    suspend fun completeProfile(
        setupToken: String,
        displayName: String,
        avatarUrl: String?,
    ): Result<Unit> {
        return try {
            val response = api.completeProfile(
                CompleteProfileRequest(
                    setupToken = setupToken,
                    displayName = displayName,
                    avatarUrl = avatarUrl,
                )
            )
            tokenStore.accessToken = response.accessToken
            tokenStore.refreshToken = response.refreshToken
            val user = api.getMe()
            tokenStore.userId = user.id
            Result.success(Unit)
        } catch (e: retrofit2.HttpException) {
            val message = when (e.code()) {
                400 -> "Session expired. Please verify your number again."
                else -> "Failed to save profile. Please try again."
            }
            Result.failure(Exception(message))
        } catch (e: Exception) {
            Result.failure(Exception("No internet connection."))
        }
    }

    /**
     * Uploads an avatar image to POST /media/avatar and returns the CDN URL.
     * Requires an authenticated user — call this post-login (e.g. from profile settings).
     */
    suspend fun uploadAvatar(uri: Uri): Result<String> {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                ?: return Result.failure(Exception("Could not read image."))
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", "avatar.jpg", requestBody)
            val response = api.uploadAvatar(part)
            Result.success(response.url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMe(): Result<UserDto> {
        return try {
            val user = api.getMe()
            tokenStore.userId = user.id
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        displayName: String? = null,
        bio: String? = null,
        presenceVisible: Boolean? = null,
        receiptsVisible: Boolean? = null,
        showTimestamps: Boolean? = null,
    ): Result<UserDto> {
        return try {
            val updated = api.updateMe(
                UpdateProfileRequest(
                    displayName = displayName,
                    bio = bio,
                    presenceVisible = presenceVisible,
                    receiptsVisible = receiptsVisible,
                    showTimestamps = showTimestamps,
                )
            )
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerPushToken(fcmToken: String) {
        try {
            api.registerPushToken(PushTokenRequest(fcmToken))
        } catch (_: Exception) {
            // Non-fatal — FCM token can be registered on next launch
        }
    }

    fun logout() {
        tokenStore.clear()
    }
}