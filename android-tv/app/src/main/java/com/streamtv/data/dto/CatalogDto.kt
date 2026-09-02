package com.streamtv.data.dto

import com.google.gson.annotations.SerializedName

data class CatalogResponseDto(
    @SerializedName("metas") val metas: List<CatalogItemDto>?
)

data class CatalogItemDto(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("name") val name: String,
    @SerializedName("poster") val poster: String?,
    @SerializedName("background") val background: String?,
    @SerializedName("posterShape") val posterShape: String?
)
