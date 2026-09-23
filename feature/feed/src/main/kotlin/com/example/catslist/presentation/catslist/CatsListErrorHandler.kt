package com.example.catslist.presentation.catslist

import com.example.catslist.domain.model.AppError
import com.example.catslist.feature.feed.R
import com.example.catslist.presentation.UiText
import com.example.catslist.presentation.toUiText

internal interface ICatsListErrorHandler {
    /** The favorites stream ended in an error and won't emit again — retrying can't help. */
    fun onFavoriteIdsFailure(error: AppError)
}

/** Covers the favorite overlay only; a failed feed load is Paging's (ADR-0024). */
internal class CatsListErrorHandler(
    private val stateHolder: ICatsListStateHolder,
) : ICatsListErrorHandler {

    /**
     * The overlay reads from Room, so [AppError.Storage] is the case that actually happens, and
     * it keeps the sentence that says what the user will see as a result — stale stars over a
     * feed that loaded fine. The rest fall back to the shared wording (ADR-0028).
     */
    override fun onFavoriteIdsFailure(error: AppError) {
        val message = when (error) {
            AppError.Storage -> UiText.Resource(R.string.catslist_error_favorites_unavailable)
            else -> error.toUiText()
        }
        stateHolder.showFavoritesUnavailable(message)
    }
}
