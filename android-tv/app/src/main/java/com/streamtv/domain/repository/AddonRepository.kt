package com.streamtv.domain.repository

import com.streamtv.data.api.AddonApi
import com.streamtv.data.api.AddonApiClient
import com.streamtv.data.local.AddonDataStore
import com.streamtv.data.mapper.toDomain
import com.streamtv.domain.model.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

class AddonRepository(
    private val dataStore: AddonDataStore
) {

    fun getInstalledAddons() = dataStore.addons

    suspend fun installAddon(name: String, url: String) {
        dataStore.addAddon(name, url)
    }

    suspend fun uninstallAddon(url: String) {
        dataStore.removeAddon(url)
    }

    suspend fun setAddonEnabled(url: String, enabled: Boolean) {
        dataStore.setAddonEnabled(url, enabled)
    }

    private fun createApi(url: String): AddonApi = AddonApiClient.createApi(url)

    suspend fun getManifest(url: String): AddonManifest? = try {
        createApi(url).getManifest().toDomain()
    } catch (e: Exception) {
        null
    }

    suspend fun getCatalogs(
        addonUrl: String,
        manifest: AddonManifest
    ): List<CatalogGroup> = coroutineScope {
        manifest.catalogs.map { catalogDef ->
            async {
                try {
                    val items = createApi(addonUrl)
                        .getCatalog(catalogDef.type, catalogDef.id)
                        .toDomain()
                    CatalogGroup(
                        addonName = manifest.name,
                        addonUrl = addonUrl,
                        catalogId = catalogDef.id,
                        catalogName = catalogDef.name,
                        metas = items
                    )
                } catch (e: Exception) {
                    CatalogGroup(
                        addonName = manifest.name,
                        addonUrl = addonUrl,
                        catalogId = catalogDef.id,
                        catalogName = catalogDef.name,
                        metas = emptyList()
                    )
                }
            }
        }.awaitAll()
    }

    suspend fun getMeta(addonUrl: String, type: String, id: String): Meta? = try {
        createApi(addonUrl).getMeta(type, id).toDomain()
    } catch (e: Exception) {
        null
    }

    suspend fun getStreams(addonUrl: String, type: String, id: String): List<Stream> = try {
        createApi(addonUrl).getStream(type, id).toDomain()
    } catch (e: Exception) {
        emptyList()
    }

    suspend fun getAllCatalogGroups(): List<CatalogGroup> = coroutineScope {
        // Collect the Flow FIRST, then filter — don't filter on the Flow itself
        val addons = dataStore.addons.first().filter { it.enabled }

        addons.map { addon ->
            async {
                try {
                    val manifest = createApi(addon.url).getManifest().toDomain()
                    if (manifest != null) {
                        getCatalogs(addon.url, manifest)
                    } else {
                        emptyList()
                    }
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }.awaitAll().flatten()
    }

    suspend fun searchAll(query: String): List<CatalogItem> = coroutineScope {
        // Collect the Flow FIRST, then filter — don't filter on the Flow itself
        val addons = dataStore.addons.first().filter { it.enabled }

        addons.map { addon ->
            async {
                try {
                    val manifest = createApi(addon.url).getManifest().toDomain()
                    if (manifest != null) {
                        val groups = getCatalogs(addon.url, manifest)
                        groups.flatMap { it.metas }
                            .filter { it.name.contains(query, ignoreCase = true) }
                    } else {
                        emptyList()
                    }
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }.awaitAll().flatten().distinctBy { it.id }
    }
}
