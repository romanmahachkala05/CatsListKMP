package com.example.catslist.data.di

import com.example.catslist.data.download.CatImageDownloader
import com.example.catslist.data.local.catDatabaseBuilder
import com.example.catslist.domain.ImageDownloader
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformDataModule: Module = module {
    single<CoroutineDispatcher>(IoDispatcher) { Dispatchers.IO }

    single { catDatabaseBuilder(androidContext()).build() }

    single<HttpClientEngine> { OkHttp.create() }

    factory<ImageDownloader> {
        CatImageDownloader(context = androidContext(), ioDispatcher = get(IoDispatcher))
    }
}
