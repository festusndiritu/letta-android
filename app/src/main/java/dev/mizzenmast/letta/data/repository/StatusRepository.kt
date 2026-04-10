package dev.mizzenmast.letta.data.repository

import dev.mizzenmast.letta.data.local.dao.StatusDao
import dev.mizzenmast.letta.data.local.entity.StatusEntity
import dev.mizzenmast.letta.data.remote.api.StatusApiService
import dev.mizzenmast.letta.data.remote.dto.CreateStatusRequest
import dev.mizzenmast.letta.data.remote.dto.StatusDto
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatusRepository @Inject constructor(
    private val api: StatusApiService,
    private val statusDao: StatusDao,
    private val tokenStore: dev.mizzenmast.letta.data.local.TokenStore,
) {
    fun observeFeed(): Flow<List<StatusEntity>> = statusDao.observeFeed()

    fun observeMine(): Flow<List<StatusEntity>> = statusDao.observeMine()

    suspend fun refreshFeed() {
        try {
            val statuses = api.getFeed()
            statusDao.upsertAll(statuses.map { it.toEntity(isMine = false) })
        } catch (_: Exception) { }
    }

    suspend fun refreshMine() {
        try {
            val statuses = api.getMine()
            statusDao.upsertAll(statuses.map { it.toEntity(isMine = true) })
        } catch (_: Exception) { }
    }

    suspend fun createStatus(
        type: String,
        content: String?,
        mediaUrl: String?,
        bgColor: String?,
    ): Result<StatusDto> {
        return try {
            val created = api.createStatus(
                CreateStatusRequest(type = type, content = content, mediaUrl = mediaUrl, bgColor = bgColor)
            )
            statusDao.upsert(created.toEntity(isMine = true))
            Result.success(created)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun viewStatus(statusId: String) {
        try { api.viewStatus(statusId) } catch (_: Exception) { }
    }

    suspend fun deleteStatus(statusId: String) {
        try {
            api.deleteStatus(statusId)
            statusDao.deleteById(statusId)
        } catch (_: Exception) { }
    }

    private fun StatusDto.toEntity(isMine: Boolean): StatusEntity = StatusEntity(
        id = id,
        userId = userId,
        type = type,
        content = content,
        mediaUrl = mediaUrl,
        bgColor = bgColor,
        createdAt = createdAt,
        isMine = isMine || userId == tokenStore.userId,
    )
}

