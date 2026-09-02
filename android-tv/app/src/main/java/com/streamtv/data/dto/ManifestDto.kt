package com.streamtv.data.dto

import com.google.gson.annotations.SerializedName

data class ManifestDto(
    @SerializedName("id") val id: String,
    @SerializedName("version") val version: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("logo") val logo: String?,
    @SerializedName("resources") val resources: List<String>,
    @SerializedName("types") val types: List<String>,
    @SerializedName("catalogs") val catalogs: List<CatalogDefDto>,
    @SerializedName("idPrefixes") val idPrefixes: List<String>?,
    @SerializedName("behaviorHints") val behaviorHints: BehaviorHintsDto?
)

data class CatalogDefDto(
    @SerializedName("type") val type: String,
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("extra") val extra: List<CatalogExtraDto>?
)

data class CatalogExtraDto(
    @SerializedName("name") val name: String,
    @SerializedName("isRequired") val isRequired: Boolean
)

data class BehaviorHintsDto(
    @SerializedName("configurable") val configurable: Boolean,
    @SerializedName("configurationRequired") val configurationRequired: Boolean
)
