package dev.mizzenmast.letta.data.remote.api

import dev.mizzenmast.letta.data.remote.dto.CallDto
import retrofit2.http.GET
import retrofit2.http.Query

interface CallApiService {
    @GET("calls")
    suspend fun getCalls(
        @Query("limit") limit: Int = 20,
        @Query("before_id") beforeId: String? = null,
    ): List<CallDto>
}

