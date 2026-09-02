package com.streamtv.domain.model

data class Stream(
    val title: String?,
    val name: String?,
    val description: String?,
    val url: String?,
    val behaviorHints: StreamBehaviorHints?
)

data class StreamBehaviorHints(
    val notWebReady: Boolean
)
