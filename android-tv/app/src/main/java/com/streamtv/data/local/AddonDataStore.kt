package com.streamtv.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "streamtv_addons")

data class StoredAddon(
    val id: String,
    val name: String,
    val url: String,
    val enabled: Boolean
)

class AddonDataStore(private val context: Context) {

    companion object {
        private val ADDON_URLS = stringPreferencesKey("addon_urls")
        private val ADDON_NAMES = stringPreferencesKey("addon_names")
        private val ADDON_ENABLED = stringPreferencesKey("addon_enabled")
    }

    val addons: Flow<List<StoredAddon>> = context.dataStore.data.map { prefs ->
        val urls = prefs[ADDON_URLS]?.split("|")?.filter { it.isNotBlank() } ?: emptyList()
        val names = prefs[ADDON_NAMES]?.split("|") ?: emptyList()
        val enabledStr = prefs[ADDON_ENABLED]?.split("|") ?: emptyList()

        urls.mapIndexed { index, url ->
            StoredAddon(
                id = url.hashCode().toString(),
                name = names.getOrElse(index) { "Addon ${index + 1}" },
                url = url,
                enabled = enabledStr.getOrElse(index) { "true" }.toBoolean()
            )
        }
    }

    suspend fun addAddon(name: String, url: String) {
        context.dataStore.edit { prefs ->
            val existingUrls = prefs[ADDON_URLS]?.split("|")?.filter { it.isNotBlank() } ?: emptyList()
            val existingNames = prefs[ADDON_NAMES]?.split("|") ?: emptyList()
            val existingEnabled = prefs[ADDON_ENABLED]?.split("|") ?: emptyList()

            if (url !in existingUrls) {
                prefs[ADDON_URLS] = (existingUrls + url).joinToString("|")
                prefs[ADDON_NAMES] = (existingNames + name).joinToString("|")
                prefs[ADDON_ENABLED] = (existingEnabled + "true").joinToString("|")
            }
        }
    }

    suspend fun removeAddon(url: String) {
        context.dataStore.edit { prefs ->
            val urls = prefs[ADDON_URLS]?.split("|")?.filter { it.isNotBlank() } ?: emptyList()
            val names = prefs[ADDON_NAMES]?.split("|") ?: emptyList()
            val enabled = prefs[ADDON_ENABLED]?.split("|") ?: emptyList()

            val index = urls.indexOf(url)
            if (index >= 0) {
                val newUrls = urls.toMutableList().apply { removeAt(index) }
                val newNames = names.toMutableList().apply { if (index < size) removeAt(index) }
                val newEnabled = enabled.toMutableList().apply { if (index < size) removeAt(index) }

                prefs[ADDON_URLS] = newUrls.joinToString("|")
                prefs[ADDON_NAMES] = newNames.joinToString("|")
                prefs[ADDON_ENABLED] = newEnabled.joinToString("|")
            }
        }
    }

    suspend fun setAddonEnabled(url: String, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            val urls = prefs[ADDON_URLS]?.split("|")?.filter { it.isNotBlank() } ?: emptyList()
            val names = prefs[ADDON_NAMES]?.split("|") ?: emptyList()
            val enabledList = prefs[ADDON_ENABLED]?.split("|")?.toMutableList() ?: mutableListOf()

            val index = urls.indexOf(url)
            if (index >= 0) {
                while (enabledList.size <= index) enabledList.add("true")
                enabledList[index] = enabled.toString()
                prefs[ADDON_ENABLED] = enabledList.joinToString("|")
            }
        }
    }
}
