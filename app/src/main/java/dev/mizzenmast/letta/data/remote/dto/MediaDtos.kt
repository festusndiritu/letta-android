package dev.mizzenmast.letta.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UploadMediaResponse(
    @SerialName("url") val url: String,
    @SerialName("mime_type") val mimeType: String,
)

