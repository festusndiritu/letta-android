package dev.mizzenmast.letta.data.repository

import dev.mizzenmast.letta.data.remote.api.MetaApiService
import dev.mizzenmast.letta.data.remote.dto.LinkPreviewDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MetaRepository @Inject constructor(
    private val api: MetaApiService,
) {
    private val cache = mutableMapOf<String, LinkPreviewDto>()

    suspend fun getPreview(url: String): Result<LinkPreviewDto> {
        cache[url]?.let { return Result.success(it) }
        return try {
            val preview = api.getPreview(url)
            cache[url] = preview
            Result.success(preview)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

