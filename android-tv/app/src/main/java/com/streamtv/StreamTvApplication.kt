package com.streamtv

import android.app.Application
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

class StreamTvApplication : Application() {

    private val dataStore: AddonDataStore by inject()
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@StreamTvApplication)
            modules(appModule)
        }

        // Run synchronously so addons exist before HomeViewModel loads
        runBlocking { installDefaultAddons() }
    }

    private suspend fun installDefaultAddons() {
        dataStore.addAddon(
            name = "School Anime Project",
            url = "http://10.0.2.2:7000"  // Emulator localhost alias
        )
    }
}
