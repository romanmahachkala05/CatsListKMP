package com.example.catslist.presentation.favoritecats

import com.example.catslist.feature.favorites.R
import com.example.catslist.presentation.UiText

internal interface IFavoriteCatsErrorHandler {
    /** The favorites stream ended in an error and won't emit again — retrying can't help. */
    fun onFavoritesFailure(error: Throwable)
}

internal class FavoriteCatsErrorHandler(
    private val stateHolder: IFavoriteCatsStateHolder,
) : IFavoriteCatsErrorHandler {

    override fun onFavoritesFailure(error: Throwable) {
        stateHolder.showError(UiText.Resource(R.string.favoritecats_error_loading_favorites))
    }
}
