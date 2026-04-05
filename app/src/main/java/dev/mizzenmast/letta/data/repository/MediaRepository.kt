package dev.mizzenmast.letta.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.mizzenmast.letta.data.remote.api.MediaApiService
import dev.mizzenmast.letta.data.remote.dto.UploadMediaResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: MediaApiService,
) {
    suspend fun upload(uri: Uri, mimeTypeOverride: String? = null): Result<UploadMediaResponse> {
        return try {
            val resolver = context.contentResolver
            val filename = resolveFileName(uri)
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return Result.failure(Exception("Could not read file."))
            if (bytes.isEmpty()) return Result.failure(Exception("File is empty."))
            val resolvedMimeType = mimeTypeOverride
                ?: resolver.getType(uri)
                ?: mimeTypeFromName(filename)
                ?: "application/octet-stream"
            val requestBody = bytes.toRequestBody(resolvedMimeType.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", filename, requestBody)
            Result.success(api.upload(part))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun resolveFileName(uri: Uri): String {
        val resolver = context.contentResolver
        val nameFromCursor = resolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
        val name = nameFromCursor?.takeIf { it.isNotBlank() }
        if (name != null) return name
        return "upload.bin"
    }

    private fun mimeTypeFromName(filename: String): String? {
        val extension = filename.substringAfterLast('.', "").lowercase()
        if (extension.isBlank()) return null
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }
}
