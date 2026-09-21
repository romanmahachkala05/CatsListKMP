package com.example.catslist.presentation.catslist

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * The feed screen's graph. The StateHolder is built inside the `viewModel` lambda rather than
 * registered on its own: that is what makes the ViewModel and its ErrorHandler share one state
 * object, which Hilt got from `@ViewModelScoped` plus a shared dependency (ADR-0026).
 */
val feedModule = module {
    viewModel {
        val stateHolder: ICatsListStateHolder = CatsListStateHolder()
        CatsListViewModel(
            stateHolder = stateHolder,
            errorHandler = CatsListErrorHandler(stateHolder),
            getCatFeed = get(),
            getFavoriteCats = get(),
            toggleFavorite = get(),
            downloadCatImage = get(),
            notifier = get(),
        )
    }
}
