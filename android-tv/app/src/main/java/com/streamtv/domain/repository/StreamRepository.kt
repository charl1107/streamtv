package com.streamtv.domain.repository

import com.streamtv.data.api.AddonApiClient
import com.streamtv.data.dto.StreamResponseDto
import com.streamtv.data.mapper.toDomain
import com.streamtv.domain.model.Stream
import com.google.gson.Gson
import okhttp3.OkHttpClient
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class StreamRepository {

    companion object {
        const val TORRENTIO_ADDON_ID = "com.stremio.torrentio.addon"
        private val httpClient = OkHttpClient.Builder().build()
        private val gson = Gson()
    }

    /**
     * Fetch streams from all local addons.
     */
    suspend fun getStreamsFromLocalAddons(
        addonUrls: List<String>,
        type: String,
        contentId: String
    ): List<Stream> = coroutineScope {
        addonUrls.map { url ->
            async {
                try {
                    AddonApiClient.createApi(url)
                        .getStream(type, contentId)
                        .toDomain()
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }.awaitAll().flatten()
    }

    /**
     * Directly call the external addon API (bypasses our backend proxy).
     * Use this when you want to talk to Torrentio directly.
     */
    suspend fun getStreamsFromTorrentio(
        type: String,
        contentId: String
    ): List<Stream> = try {
        val api = AddonApiClient.createApi("https://torrentio.strem.fun")
        api.getStream(type, contentId).toDomain()
    } catch (e: Exception) {
        println("[StreamRepo] Torrentio direct failed: ${e.message}")
        emptyList()
    }

    /**
     * Fetch streams through our backend's proxy endpoint.
     */
    suspend fun getStreamsFromProxy(
        backendUrl: String,
        addonId: String,
        type: String,
        contentId: String
    ): List<Stream> = try {
        val url = "$backendUrl/addons/$addonId/streams/$type/$contentId.json"
        val request = okhttp3.Request.Builder()
            .url(url)
            .addHeader("User-Agent", "StreamTV/1.0")
            .build()

        val response = httpClient.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response")
        val dto = gson.fromJson(body, StreamResponseDto::class.java)
        dto.toDomain()
    } catch (e: Exception) {
        println("[StreamRepo] Proxy fetch failed for $addonId: ${e.message}")
        emptyList()
    }
}
