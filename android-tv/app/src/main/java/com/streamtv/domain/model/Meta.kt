package com.streamtv.domain.model

data class Meta(
    val id: String,
    val type: String,
    val name: String,
    val description: String?,
    val poster: String?,
    val background: String?,
    val logo: String?,
    val runtime: String?,
    val year: String?,
    val genres: List<String>,
    val imdbId: String?,
    val videos: List<VideoMeta>
)

data class VideoMeta(
    val id: String,
    val title: String,
    val season: Int,
    val episode: Int,
    val duration: Long?,
    val released: String?
)
