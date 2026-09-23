package com.example.catslist.presentation.favoritecats

import com.example.catslist.domain.model.AppError
import com.example.catslist.feature.favorites.R
import com.example.catslist.presentation.UiText
import com.example.catslist.presentation.toUiText

internal interface IFavoriteCatsErrorHandler {
    /** The favorites stream ended in an error and won't emit again — retrying can't help. */
    fun onFavoritesFailure(error: AppError)
}

internal class FavoriteCatsErrorHandler(
    private val stateHolder: IFavoriteCatsStateHolder,
) : IFavoriteCatsErrorHandler {

    /**
     * One case gets screen-specific wording and the rest take the shared default. The screen
     * knows what it was reading when the database failed — "your favorites", not "your saved
     * cats" — and nothing else about a 5xx or a timeout is worth saying twice (ADR-0028).
     */
    override fun onFavoritesFailure(error: AppError) {
        val message = when (error) {
            AppError.Storage -> UiText.AndroidResource(R.string.favoritecats_error_loading_favorites)
            else -> error.toUiText()
        }
        stateHolder.showError(message)
    }
}
