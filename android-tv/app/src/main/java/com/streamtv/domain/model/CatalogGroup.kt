package com.streamtv.domain.model

data class CatalogGroup(
    val addonName: String,
    val addonUrl: String,
    val catalogId: String,
    val catalogName: String,
    val metas: List<CatalogItem>
)

data class CatalogItem(
    val id: String,
    val type: String,
    val name: String,
    val poster: String?,
    val background: String?
)
