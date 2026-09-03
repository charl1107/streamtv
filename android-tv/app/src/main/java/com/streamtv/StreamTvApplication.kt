package com.streamtv

import android.app.Application
import android.util.Log
import com.streamtv.di.appModule
import com.streamtv.data.local.AddonDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

private const val TAG = "StreamTvApp"

class StreamTvApplication : Application() {

    private val dataStore: AddonDataStore by inject()
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@StreamTvApplication)
            modules(appModule)
        }

        // Install synchronously so addons exist before HomeViewModel loads
        runBlocking {
            try {
                installDefaultAddons()
                Log.d(TAG, "Default addons installed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to install default addons", e)
            }
        }

        // Verify it worked
        applicationScope.launch {
            val addons = dataStore.addons
            Log.d(TAG, "Addon store has ${addons.size} entries")
        }
    }

    private suspend fun installDefaultAddons() {
        dataStore.addAddon(
            name = "School Anime Project",
            url = "http://10.0.2.2:7000"  // Emulator localhost alias
        )
    }
}
