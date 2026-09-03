package com.streamtv.domain.repository

import android.util.Log
import com.streamtv.data.api.AddonApi
import com.streamtv.data.api.AddonApiClient
import com.streamtv.data.local.AddonDataStore
import com.streamtv.data.mapper.toDomain
import com.streamtv.domain.model.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

private const val TAG = "AddonRepository"
private const val FALLBACK_URL = "http://10.0.2.2:7000"

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
        var addons = dataStore.addons.first().filter { it.enabled }

        // Fallback: if DataStore is empty, try the hardcoded URL directly
        if (addons.isEmpty()) {
            Log.w(TAG, "No addons in DataStore, trying fallback URL: $FALLBACK_URL")
            addons = listOf(
                com.streamtv.data.local.StoredAddon(
                    id = "fallback",
                    name = "School Anime Project",
                    url = FALLBACK_URL,
                    enabled = true
                )
            )
        }

        val results = addons.map { addon ->
            async {
                try {
                    val manifest = createApi(addon.url).getManifest().toDomain()
                    if (manifest != null) {
                        Log.d(TAG, "Got manifest from ${addon.url}: ${manifest.name}")
                        getCatalogs(addon.url, manifest)
                    } else {
                        throw Exception("Cannot reach addon at ${addon.url}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to fetch from ${addon.url}: ${e.message}")
                    throw Exception("Cannot connect to ${addon.name} at ${addon.url} — is the server running?")
                }
            }
        }.awaitAll()

        val allGroups = results.flatten()
        if (allGroups.isEmpty()) {
            throw Exception("Addon returned no catalogs. Check server logs.")
        }
        allGroups
    }

    suspend fun searchAll(query: String): List<CatalogItem> = coroutineScope {
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
