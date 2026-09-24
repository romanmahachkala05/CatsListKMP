package com.example.catslist.data.di

import com.example.catslist.data.download.DesktopImageDownloader
import com.example.catslist.data.local.catDatabaseBuilder
import com.example.catslist.data.network.DesktopNetworkMonitor
import com.example.catslist.data.remote.catOkHttpClient
import com.example.catslist.domain.ImageDownloader
import com.example.catslist.domain.NetworkMonitor
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformDataModule: Module = module {
    single<CoroutineDispatcher>(IoDispatcher) { Dispatchers.IO }

    single { catDatabaseBuilder().build() }

    // The same client and engine as Android: OkHttp runs on both, so the HTTP stack — and its
    // connection pool — is shared and only its construction is per-platform (ADR-0030).
    single { catOkHttpClient() }
    single<HttpClientEngine> { OkHttp.create { preconfigured = get<OkHttpClient>() } }

    single<NetworkMonitor> { DesktopNetworkMonitor(ioDispatcher = get(IoDispatcher)) }

    factory<ImageDownloader> { DesktopImageDownloader(client = get(), ioDispatcher = get(IoDispatcher)) }
}
