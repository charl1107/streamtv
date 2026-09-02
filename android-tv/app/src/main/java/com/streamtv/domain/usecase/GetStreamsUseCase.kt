package com.streamtv.domain.usecase

import com.streamtv.domain.model.Stream
import com.streamtv.domain.repository.AddonRepository
import com.streamtv.domain.repository.StreamRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

class GetStreamsUseCase(
    private val addonRepository: AddonRepository,
    private val streamRepository: StreamRepository
) {
    companion object {
        private const val BACKEND_URL = "http://10.0.2.2:7000"
    }

    /**
     * Fetch streams from all sources:
     * 1. Local addons (your school project server)
     * 2. External addons (Torrentio via backend proxy)
     *
     * @param type Content type (movie, series, anime)
     * @param contentId Content ID (kitsu:1:1 for local, tt1375666 for IMDB/Torrentio)
     * @param imdbId Optional IMDB ID for Torrentio (tt...)
     */
    suspend operator fun invoke(
        type: String,
        contentId: String,
        imdbId: String? = null
    ): List<Stream> = coroutineScope {
        val localStreams = async {
            // Collect the Flow first, THEN filter — don't filter on the Flow itself
            val addons = addonRepository.getInstalledAddons().first()
                .filter { it.enabled }
            streamRepository.getStreamsFromLocalAddons(
                addonUrls = addons.map { it.url },
                type = type,
                contentId = contentId
            )
        }

        val torrentioStreams = async {
            // Torrentio uses IMDB IDs (tt...). If we have one, fetch from Torrentio.
            val torrentioId = imdbId ?: contentId
            if (torrentioId.startsWith("tt")) {
                streamRepository.getStreamsFromTorrentio(type, torrentioId)
            } else {
                emptyList()
            }
        }

        // Combine all streams, with local first, then Torrentio
        val local = localStreams.await()
        val external = torrentioStreams.await()

        local + external
    }
}
