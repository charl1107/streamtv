package com.streamtv.domain.model

data class AddonManifest(
    val id: String,
    val version: String,
    val name: String,
    val description: String,
    val logo: String?,
    val resources: List<String>,
    val types: List<String>,
    val catalogs: List<CatalogDef>,
    val idPrefixes: List<String>?,
    val behaviorHints: BehaviorHints?
)

data class CatalogDef(
    val type: String,
    val id: String,
    val name: String,
    val extra: List<CatalogExtra>?
)

data class CatalogExtra(
    val name: String,
    val isRequired: Boolean
)

data class BehaviorHints(
    val configurable: Boolean,
    val configurationRequired: Boolean
)
