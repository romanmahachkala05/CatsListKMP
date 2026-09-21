package com.example.catslist.presentation.favoritecats

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * The favorites screen's graph. The StateHolder is built inside the `viewModel` lambda rather
 * than registered on its own: that is what makes the ViewModel and its ErrorHandler share one
 * state object, which Hilt got from `@ViewModelScoped` plus a shared dependency (ADR-0026).
 */
val favoritesModule = module {
    viewModel {
        val stateHolder: IFavoriteCatsStateHolder = FavoriteCatsStateHolder()
        FavoriteCatsViewModel(
            stateHolder = stateHolder,
            errorHandler = FavoriteCatsErrorHandler(stateHolder),
            getFavoriteCats = get(),
            removeFavorite = get(),
            downloadCatImage = get(),
            notifier = get(),
        )
    }
}
