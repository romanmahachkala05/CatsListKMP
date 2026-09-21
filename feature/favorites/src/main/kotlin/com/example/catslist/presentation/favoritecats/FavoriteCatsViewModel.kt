package com.example.catslist.presentation.favoritecats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catslist.domain.usecase.DownloadCatImageUseCase
import com.example.catslist.domain.usecase.GetFavoriteCatsUseCase
import com.example.catslist.domain.usecase.RemoveFavoriteUseCase
import com.example.catslist.presentation.FAVORITE_FAILED
import com.example.catslist.presentation.RetryableFlow
import com.example.catslist.presentation.SnackbarNotifier
import com.example.catslist.presentation.StateOwner
import com.example.catslist.presentation.downloadCat
import com.example.catslist.presentation.launchCatching
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class FavoriteCatsViewModel(
    private val stateHolder: IFavoriteCatsStateHolder,
    private val errorHandler: IFavoriteCatsErrorHandler,
    getFavoriteCats: GetFavoriteCatsUseCase,
    private val removeFavorite: RemoveFavoriteUseCase,
    private val downloadCatImage: DownloadCatImageUseCase,
    private val notifier: SnackbarNotifier,
) : ViewModel(),
    StateOwner<FavoriteCatsState> by stateHolder {

    // Without this a throwing Room query would escape viewModelScope and kill the process.
    private val favorites = RetryableFlow(source = { getFavoriteCats() }, onFailure = errorHandler::onFavoritesFailure)

    init {
        favorites.flow
            .onEach(stateHolder::showFavorites)
            .launchIn(viewModelScope)
    }

    override fun onCleared() = stateHolder.reset()

    fun onEvent(event: FavoriteCatsEvent) {
        when (event) {
            FavoriteCatsEvent.Retry -> retry()

            is FavoriteCatsEvent.RemoveFavorite -> launchCatching(
                onFailure = { notifier.showMessage(FAVORITE_FAILED) },
            ) {
                removeFavorite(event.cat)
            }

            is FavoriteCatsEvent.Download -> downloadCat(event.cat, downloadCatImage, notifier)
        }
    }

    private fun retry() {
        stateHolder.showLoading()
        favorites.retry()
    }
}
