package com.example.catslist.data.di

import com.example.catslist.data.error.ErrorMapper
import com.example.catslist.data.local.CatDatabase
import com.example.catslist.data.remote.CatApiService
import com.example.catslist.data.remote.KtorCatApiService
import com.example.catslist.data.remote.catHttpClient
import com.example.catslist.data.repository.CatRepositoryImpl
import com.example.catslist.domain.repository.CatRepository
import com.example.catslist.domain.usecase.DownloadCatImageUseCase
import com.example.catslist.domain.usecase.GetCatFeedUseCase
import com.example.catslist.domain.usecase.GetFavoriteCatsUseCase
import com.example.catslist.domain.usecase.RemoveFavoriteUseCase
import com.example.catslist.domain.usecase.ToggleFavoriteUseCase
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Replaces Hilt's `@Dispatcher(IO)`: Koin qualifies by value, not by annotation. */
val IoDispatcher = named("io")

/**
 * What a platform has to answer for: where the database file lives, which HTTP engine to
 * drive (and the [okhttp3.OkHttpClient] it and Coil share, on the platforms that have one),
 * how to download an image, and how [com.example.catslist.domain.NetworkMonitor] is
 * implemented. Everything else in this module is the same everywhere.
 */
expect val platformDataModule: Module

/**
 * Everything `:core:data` offers the app. `single` is Hilt's `@Singleton`; `factory` is its
 * unscoped default, so the split below mirrors the bindings this replaces (ADR-0026).
 */
val dataModule = module {
    includes(platformDataModule)

    factory { get<CatDatabase>().catDao() }

    single { catHttpClient(get()) }
    factory<CatApiService> { KtorCatApiService(get()) }

    single { ErrorMapper(networkMonitor = get()) }

    single<CatRepository> {
        CatRepositoryImpl(catDao = get(), catApiService = get(), errorMapper = get())
    }

    factory { GetCatFeedUseCase(get()) }
    factory { GetFavoriteCatsUseCase(get()) }
    factory { ToggleFavoriteUseCase(get()) }
    factory { RemoveFavoriteUseCase(get()) }
    factory { DownloadCatImageUseCase(get()) }
}
