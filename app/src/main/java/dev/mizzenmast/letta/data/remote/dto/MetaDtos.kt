package dev.mizzenmast.letta.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LinkPreviewDto(
    @SerialName("url") val url: String,
    @SerialName("title") val title: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("site_name") val siteName: String? = null,
)

