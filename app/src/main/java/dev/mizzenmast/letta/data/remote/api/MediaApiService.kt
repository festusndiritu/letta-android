package dev.mizzenmast.letta.data.remote.api

import dev.mizzenmast.letta.data.remote.dto.UploadMediaResponse
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface MediaApiService {
    @Multipart
    @POST("media/upload")
    suspend fun upload(@Part file: MultipartBody.Part): UploadMediaResponse
}

