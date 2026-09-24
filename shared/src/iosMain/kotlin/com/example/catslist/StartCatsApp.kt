package com.example.catslist

import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import org.koin.core.context.startKoin

/**
 * The iOS app's `App.onCreate`: starts Koin with the module list every platform shares, and
 * gives Coil a client on the graph's one URLSession engine — iOS's form of the single shared
 * client of ADR-0030. Swift calls it once, before showing [MainViewController].
 */
fun startCatsApp() {
    val koin = startKoin { modules(appModules) }.koin

    // A client of its own, not the API's: that one resolves paths against TheCatAPI and throws
    // on a non-2xx, neither of which an image request wants. The engine underneath is shared.
    SingletonImageLoader.setSafe { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory(httpClient = { HttpClient(koin.get<HttpClientEngine>()) })) }
            .build()
    }
}
