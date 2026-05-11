package dev.mizzenmast.letta.data.repository

import dev.mizzenmast.letta.data.local.dao.CallDao
import dev.mizzenmast.letta.data.local.entity.CallEntity
import dev.mizzenmast.letta.data.remote.api.CallApiService
import dev.mizzenmast.letta.data.remote.dto.CallDto
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallRepository @Inject constructor(
    private val api: CallApiService,
    private val callDao: CallDao,
) {
    fun observeCalls(): Flow<List<CallEntity>> = callDao.observeAll()

    suspend fun refreshCalls(limit: Int = 20, beforeId: String? = null) {
        try {
            val calls = api.getCalls(limit = limit, beforeId = beforeId)
            callDao.upsertAll(calls.map { it.toEntity() })
        } catch (_: Exception) { }
    }

    suspend fun upsertCall(call: CallEntity) {
        callDao.upsert(call)
    }

    suspend fun clearCalls() {
        callDao.clearAll()
    }

    suspend fun deleteCall(id: String) {
        callDao.deleteById(id)
    }

    private fun CallDto.toEntity(): CallEntity = CallEntity(
        id = id,
        conversationId = conversationId,
        callerId = callerId,
        calleeId = calleeId,
        type = type,
        createdAt = createdAt,
        endedAt = endedAt,
        status = status,
    )
}

