package com.streamtv.domain.model

data class Addon(
    val id: String,
    val name: String,
    val description: String,
    val url: String,
    val manifest: AddonManifest? = null,
    val enabled: Boolean = true
)
