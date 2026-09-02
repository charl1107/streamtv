package com.streamtv.domain.usecase

import com.streamtv.domain.model.CatalogGroup
import com.streamtv.domain.repository.AddonRepository

class GetCatalogUseCase(
    private val addonRepository: AddonRepository
) {
    suspend operator fun invoke(): List<CatalogGroup> {
        return addonRepository.getAllCatalogGroups()
    }
}
