package com.example.catslist.presentation.di

import com.example.catslist.presentation.DefaultSnackbarNotifier
import com.example.catslist.presentation.SnackbarNotifier
import org.koin.dsl.module

/** Shared presentation collaborators. */
val uiModule = module {
    /** A `single`: this is the one instance every screen and the Snackbar host share. */
    single<SnackbarNotifier> { DefaultSnackbarNotifier() }
}
