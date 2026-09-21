package com.example.catslist.data.di

import com.example.catslist.data.download.CatImageDownloader
import com.example.catslist.data.local.CatDatabase
import com.example.catslist.data.local.catDatabaseBuilder
import com.example.catslist.data.remote.CatApiService
import com.example.catslist.data.repository.CatRepositoryImpl
import com.example.catslist.domain.ImageDownloader
import com.example.catslist.domain.repository.CatRepository
import com.example.catslist.domain.usecase.DownloadCatImageUseCase
import com.example.catslist.domain.usecase.GetCatFeedUseCase
import com.example.catslist.domain.usecase.GetFavoriteCatsUseCase
import com.example.catslist.domain.usecase.RemoveFavoriteUseCase
import com.example.catslist.domain.usecase.ToggleFavoriteUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

private const val CAT_API_BASE_URL = "https://api.thecatapi.com"

/** Replaces Hilt's `@Dispatcher(IO)`: Koin qualifies by value, not by annotation. */
val IoDispatcher = named("io")

/**
 * kotlinx.serialization rejects unknown keys by default, so a field added upstream would
 * start failing every response. Tolerating them is the safe default for a wire model we
 * do not own.
 */
private val json = Json { ignoreUnknownKeys = true }

/**
 * Everything `:core:data` offers the app. `single` is Hilt's `@Singleton`; `factory` is its
 * unscoped default, so the split below mirrors the bindings this replaces (ADR-0026).
 */
val dataModule = module {
    single<CoroutineDispatcher>(IoDispatcher) { Dispatchers.IO }

    single { catDatabaseBuilder(androidContext()).build() }
    factory { get<CatDatabase>().catDao() }

    single {
        Retrofit.Builder()
            .baseUrl(CAT_API_BASE_URL)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
    factory { get<Retrofit>().create(CatApiService::class.java) }

    single<CatRepository> { CatRepositoryImpl(catDao = get(), catApiService = get()) }
    factory<ImageDownloader> {
        CatImageDownloader(context = androidContext(), ioDispatcher = get(IoDispatcher))
    }

    factory { GetCatFeedUseCase(get()) }
    factory { GetFavoriteCatsUseCase(get()) }
    factory { ToggleFavoriteUseCase(get()) }
    factory { RemoveFavoriteUseCase(get()) }
    factory { DownloadCatImageUseCase(get()) }
}
