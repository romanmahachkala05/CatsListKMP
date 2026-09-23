package com.example.catslist

import androidx.compose.runtime.Composable
import com.example.catslist.data.di.dataModule
import com.example.catslist.presentation.catslist.feedModule
import com.example.catslist.presentation.di.uiModule
import com.example.catslist.presentation.favoritecats.favoritesModule
import com.example.catslist.presentation.navigation.CatsNavDisplay
import com.example.catslist.presentation.theme.CatsListTheme
import org.koin.compose.koinInject
import org.koin.core.module.Module

/** The whole app's UI, the same on every platform. Koin must already be started. */
@Composable
fun CatsApp() {
    CatsListTheme {
        CatsNavDisplay(notifier = koinInject())
    }
}

/**
 * Every Koin module the app needs, for each platform's entry point to start Koin with. Koin
 * has no aggregation step, so a module missing here fails at startup rather than at compile
 * time (ADR-0026) — which is why the list lives in one place instead of once per app.
 */
val appModules: List<Module> = listOf(dataModule, uiModule, feedModule, favoritesModule)
