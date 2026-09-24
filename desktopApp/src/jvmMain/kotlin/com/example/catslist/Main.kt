package com.example.catslist

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.example.catslist.desktopApp.resources.Res
import com.example.catslist.desktopApp.resources.app_icon
import okhttp3.OkHttpClient
import org.jetbrains.compose.resources.painterResource
import org.koin.core.context.startKoin

/**
 * Starts Koin with the module list every platform shares, gives Coil the graph's one
 * [OkHttpClient] (ADR-0030, as Android's `App` does), and opens the window.
 */
fun main() {
    val koin = startKoin { modules(appModules) }.koin

    // Registering the fetcher explicitly is what keeps Coil off a client of its own; see
    // `App.newImageLoader` on Android for why this one is matched first.
    SingletonImageLoader.setSafe { context ->
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { koin.get<OkHttpClient>() })) }
            .build()
    }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "CatsList",
            icon = painterResource(Res.drawable.app_icon),
            // Phone-shaped: the screens are a single column of cards, laid out for a phone.
            state = rememberWindowState(width = 480.dp, height = 900.dp),
        ) {
            CatsApp()
        }
    }
}
