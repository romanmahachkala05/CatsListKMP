package com.example.catslist.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Wire model for TheCatAPI's `/v1/images/search` response. No `favorite` field — the API doesn't know about it. */
@Serializable
data class CatDto(
    @SerialName("id") val id: String,
    @SerialName("url") val url: String,
    @SerialName("width") val width: Int,
    @SerialName("height") val height: Int,
)
