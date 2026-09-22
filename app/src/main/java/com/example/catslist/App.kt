package com.example.catslist

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.example.catslist.data.di.dataModule
import com.example.catslist.presentation.catslist.feedModule
import com.example.catslist.presentation.di.uiModule
import com.example.catslist.presentation.favoritecats.favoritesModule
import okhttp3.OkHttpClient
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

/**
 * The one place the module graph is assembled. Hilt aggregated `@InstallIn` modules across
 * Gradle modules by itself; Koin has no such step, so each module is listed here by hand and
 * a missing one fails at startup rather than at compile time (ADR-0026).
 *
 * Also the place Coil's singleton loader is built, so the app has exactly one [OkHttpClient]
 * rather than one per library (ADR-0030). Wiring a singleton across two libraries is
 * composition-root work, which is why it lives here and not in `:core:designsystem`.
 */
class App :
    Application(),
    SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            // Debug builds only: the logger reflects on every definition it prints.
            if (BuildConfig.DEBUG) androidLogger()
            androidContext(this@App)
            modules(dataModule, uiModule, feedModule, favoritesModule)
        }
    }

    /**
     * Called by Coil on first image load, well after [onCreate] — the client Koin hands back
     * here is built on whichever thread that turns out to be, not the main thread.
     *
     * Registering the fetcher explicitly is what removes the second client. `coil-network-okhttp`
     * also registers one through a `ServiceLoader`, built over an `OkHttpClient()` of its own —
     * but `RealImageLoader` assembles the builder's own components ahead of the ServiceLoader's,
     * so this one is matched first for every http(s) request and that default is never built.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context)
        .components {
            add(OkHttpNetworkFetcherFactory(callFactory = { get<OkHttpClient>() }))
        }
        .build()
}
