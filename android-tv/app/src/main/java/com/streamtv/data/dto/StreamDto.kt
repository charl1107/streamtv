package com.streamtv.data.dto

import com.google.gson.annotations.SerializedName

data class StreamResponseDto(
    @SerializedName("streams") val streams: List<StreamDto>?
)

data class StreamDto(
    @SerializedName("title") val title: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("behaviorHints") val behaviorHints: StreamBehaviorHintsDto?
)

data class StreamBehaviorHintsDto(
    @SerializedName("notWebReady") val notWebReady: Boolean?
)
