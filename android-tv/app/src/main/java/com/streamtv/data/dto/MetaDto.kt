package com.streamtv.data.dto

import com.google.gson.annotations.SerializedName

data class MetaResponseDto(
    @SerializedName("meta") val meta: MetaDto?
)

data class MetaDto(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("poster") val poster: String?,
    @SerializedName("background") val background: String?,
    @SerializedName("logo") val logo: String?,
    @SerializedName("runtime") val runtime: String?,
    @SerializedName("year") val year: String?,
    @SerializedName("genres") val genres: List<String>?,
    @SerializedName("imdbId") val imdbId: String?,
    @SerializedName("videos") val videos: List<VideoDto>?
)

data class VideoDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("season") val season: Int,
    @SerializedName("episode") val episode: Int,
    @SerializedName("duration") val duration: Long?,
    @SerializedName("released") val released: String?
)
