package dev.mizzenmast.letta.data.remote.api

import dev.mizzenmast.letta.data.remote.dto.LinkPreviewDto
import retrofit2.http.GET
import retrofit2.http.Query

interface MetaApiService {
    @GET("meta/preview")
    suspend fun getPreview(@Query("url") url: String): LinkPreviewDto
}

