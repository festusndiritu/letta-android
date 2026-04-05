package dev.mizzenmast.letta.data.remote.api

import dev.mizzenmast.letta.data.remote.dto.CompleteProfileRequest
import dev.mizzenmast.letta.data.remote.dto.PushTokenRequest
import dev.mizzenmast.letta.data.remote.dto.RefreshRequest
import dev.mizzenmast.letta.data.remote.dto.RequestOtpRequest
import dev.mizzenmast.letta.data.remote.dto.RequestOtpResponse
import dev.mizzenmast.letta.data.remote.dto.TokenResponse
import dev.mizzenmast.letta.data.remote.dto.UpdateProfileRequest
import dev.mizzenmast.letta.data.remote.dto.UploadAvatarResponse
import dev.mizzenmast.letta.data.remote.dto.UserDto
import dev.mizzenmast.letta.data.remote.dto.VerifyOtpRequest
import dev.mizzenmast.letta.data.remote.dto.VerifyOtpResponse
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part

interface AuthApiService {

    @POST("auth/request-otp")
    suspend fun requestOtp(@Body body: RequestOtpRequest): RequestOtpResponse

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body body: VerifyOtpRequest): VerifyOtpResponse

    @POST("auth/complete-profile")
    suspend fun completeProfile(@Body body: CompleteProfileRequest): TokenResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): TokenResponse

    @Multipart
    @POST("media/avatar")
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): UploadAvatarResponse

    @GET("auth/users/me")
    suspend fun getMe(): UserDto

    @PATCH("auth/users/me")
    suspend fun updateMe(@Body body: UpdateProfileRequest): UserDto

    @POST("auth/users/me/push-token")
    suspend fun registerPushToken(@Body body: PushTokenRequest)
}