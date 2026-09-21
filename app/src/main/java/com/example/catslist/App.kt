package com.example.catslist

import android.app.Application
import com.example.catslist.data.di.dataModule
import com.example.catslist.presentation.catslist.feedModule
import com.example.catslist.presentation.di.uiModule
import com.example.catslist.presentation.favoritecats.favoritesModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

/**
 * The one place the module graph is assembled. Hilt aggregated `@InstallIn` modules across
 * Gradle modules by itself; Koin has no such step, so each module is listed here by hand and
 * a missing one fails at startup rather than at compile time (ADR-0026).
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            // Debug builds only: the logger reflects on every definition it prints.
            if (BuildConfig.DEBUG) androidLogger()
            androidContext(this@App)
            modules(dataModule, uiModule, feedModule, favoritesModule)
        }
    }
}
