package com.streamtv.di

import com.streamtv.data.local.AddonDataStore
import com.streamtv.domain.repository.AddonRepository
import com.streamtv.domain.repository.StreamRepository
import com.streamtv.domain.usecase.GetCatalogUseCase
import com.streamtv.domain.usecase.GetMetaUseCase
import com.streamtv.domain.usecase.GetStreamsUseCase
import com.streamtv.ui.viewmodel.DetailViewModel
import com.streamtv.ui.viewmodel.HomeViewModel
import com.streamtv.ui.viewmodel.PlayerViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // Data
    single { AddonDataStore(androidContext()) }

    // Repositories
    single { AddonRepository(get()) }
    single { StreamRepository() }

    // Use Cases
    single { GetCatalogUseCase(get()) }
    single { GetMetaUseCase(get()) }
    single { GetStreamsUseCase(get(), get()) }

    // ViewModels
    viewModel { HomeViewModel(get()) }
    viewModel { params -> DetailViewModel(get(), get(), params.get(), params.get(), params.get()) }
    viewModel { params -> PlayerViewModel(get(), params.get(), params.get(), params.get()) }
}
