package com.example.catslist.presentation.catslist

internal interface ICatsListErrorHandler {
    /** The favorites stream ended in an error and won't emit again — retrying can't help. */
    fun onFavoriteIdsFailure(error: Throwable)
}

/** Covers the favorite overlay only; a failed feed load is Paging's (ADR-0024). */
internal class CatsListErrorHandler(
    private val stateHolder: ICatsListStateHolder,
) : ICatsListErrorHandler {

    override fun onFavoriteIdsFailure(error: Throwable) {
        stateHolder.showFavoritesUnavailable()
    }
}
