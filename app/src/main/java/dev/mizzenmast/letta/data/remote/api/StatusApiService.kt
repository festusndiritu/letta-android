package dev.mizzenmast.letta.data.remote.api

import dev.mizzenmast.letta.data.remote.dto.CreateStatusRequest
import dev.mizzenmast.letta.data.remote.dto.StatusDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface StatusApiService {
    @POST("statuses")
    suspend fun createStatus(@Body body: CreateStatusRequest): StatusDto

    @GET("statuses/feed")
    suspend fun getFeed(): List<StatusDto>

    @GET("statuses/mine")
    suspend fun getMine(): List<StatusDto>

    @POST("statuses/{id}/view")
    suspend fun viewStatus(@Path("id") statusId: String)

    @DELETE("statuses/{id}")
    suspend fun deleteStatus(@Path("id") statusId: String)
}

