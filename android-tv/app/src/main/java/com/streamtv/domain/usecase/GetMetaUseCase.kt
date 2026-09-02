package com.streamtv.domain.usecase

import com.streamtv.domain.model.Meta
import com.streamtv.domain.repository.AddonRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

class GetMetaUseCase(
    private val addonRepository: AddonRepository
) {
    suspend operator fun invoke(type: String, id: String): Meta? = coroutineScope {
        // Collect the Flow FIRST, then filter — don't filter on the Flow itself
        val addons = addonRepository.getInstalledAddons().first()
            .filter { it.enabled }

        val results = addons.map { addon ->
            async {
                addonRepository.getMeta(addon.url, type, id)
            }
        }.awaitAll()

        results.firstOrNull()
    }
}
