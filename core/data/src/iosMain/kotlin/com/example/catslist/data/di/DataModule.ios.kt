package com.example.catslist.data.di

import com.example.catslist.data.download.IosImageDownloader
import com.example.catslist.data.local.catDatabaseBuilder
import com.example.catslist.data.network.IosNetworkMonitor
import com.example.catslist.data.remote.REQUEST_TIMEOUT_SECONDS
import com.example.catslist.domain.ImageDownloader
import com.example.catslist.domain.NetworkMonitor
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformDataModule: Module = module {
    single<CoroutineDispatcher>(IoDispatcher) { Dispatchers.IO }

    single { catDatabaseBuilder().build() }

    // URLSession, with the cap OkHttp's `callTimeout` sets elsewhere: `ForResource` bounds the
    // whole request, `ForRequest` the wait between two pieces of it.
    single<HttpClientEngine> {
        Darwin.create {
            configureSession {
                timeoutIntervalForRequest = REQUEST_TIMEOUT_SECONDS.toDouble()
                timeoutIntervalForResource = REQUEST_TIMEOUT_SECONDS.toDouble()
            }
        }
    }

    single<NetworkMonitor> { IosNetworkMonitor() }

    factory<ImageDownloader> { IosImageDownloader(client = get(), ioDispatcher = get(IoDispatcher)) }
}
