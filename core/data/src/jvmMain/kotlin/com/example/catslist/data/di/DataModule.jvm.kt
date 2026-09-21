package com.example.catslist.data.di

import com.example.catslist.data.download.DesktopImageDownloader
import com.example.catslist.data.local.catDatabaseBuilder
import com.example.catslist.domain.ImageDownloader
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformDataModule: Module = module {
    single<CoroutineDispatcher>(IoDispatcher) { Dispatchers.IO }

    single { catDatabaseBuilder().build() }

    // The same engine as Android: OkHttp runs on both, so the HTTP stack is shared and only
    // its construction is per-platform.
    single<HttpClientEngine> { OkHttp.create() }

    factory<ImageDownloader> { DesktopImageDownloader(client = get(), ioDispatcher = get(IoDispatcher)) }
}
