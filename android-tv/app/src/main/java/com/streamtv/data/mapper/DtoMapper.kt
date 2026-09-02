package com.streamtv.data.mapper

import com.streamtv.data.dto.*
import com.streamtv.domain.model.*

// --- Manifest mapping ---

fun ManifestDto.toDomain() = AddonManifest(
    id = id,
    version = version,
    name = name,
    description = description,
    logo = logo,
    resources = resources,
    types = types,
    catalogs = catalogs.map { it.toDomain() },
    idPrefixes = idPrefixes,
    behaviorHints = behaviorHints?.toDomain()
)

fun CatalogDefDto.toDomain() = CatalogDef(
    type = type,
    id = id,
    name = name,
    extra = extra?.map { CatalogExtra(it.name, it.isRequired) }
)

fun BehaviorHintsDto.toDomain() = BehaviorHints(
    configurable = configurable,
    configurationRequired = configurationRequired
)

// --- Catalog mapping ---

fun CatalogResponseDto.toDomain() = metas?.map { it.toDomain() } ?: emptyList()

fun CatalogItemDto.toDomain() = CatalogItem(
    id = id,
    type = type,
    name = name,
    poster = poster,
    background = background
)

// --- Meta mapping ---

fun MetaResponseDto.toDomain(): Meta? = meta?.let { m ->
    Meta(
        id = m.id,
        type = m.type,
        name = m.name,
        description = m.description,
        poster = m.poster,
        background = m.background,
        logo = m.logo,
        runtime = m.runtime,
        year = m.year,
        genres = m.genres ?: emptyList(),
        imdbId = m.imdbId,
        videos = m.videos?.map { it.toDomain() } ?: emptyList()
    )
}

fun VideoDto.toDomain() = VideoMeta(
    id = id,
    title = title,
    season = season,
    episode = episode,
    duration = duration,
    released = released
)

// --- Stream mapping ---

fun StreamResponseDto.toDomain() = streams?.map { it.toDomain() } ?: emptyList()

fun StreamDto.toDomain() = Stream(
    title = title,
    name = name,
    description = description,
    url = url,
    behaviorHints = StreamBehaviorHints(
        notWebReady = behaviorHints?.notWebReady ?: false
    )
)
