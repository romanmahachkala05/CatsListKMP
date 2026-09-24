package com.example.catslist.presentation.catslist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.catslist.domain.model.Cat
import com.example.catslist.domain.usecase.DownloadCatImageUseCase
import com.example.catslist.domain.usecase.GetCatFeedUseCase
import com.example.catslist.domain.usecase.GetFavoriteCatsUseCase
import com.example.catslist.domain.usecase.ToggleFavoriteUseCase
import com.example.catslist.presentation.FAVORITE_FAILED
import com.example.catslist.presentation.SnackbarNotifier
import com.example.catslist.presentation.StateOwner
import com.example.catslist.presentation.asAppError
import com.example.catslist.presentation.downloadCat
import com.example.catslist.presentation.launchCatching
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

internal class CatsListViewModel(
    private val stateHolder: ICatsListStateHolder,
    private val errorHandler: ICatsListErrorHandler,
    getCatFeed: GetCatFeedUseCase,
    getFavoriteCats: GetFavoriteCatsUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val downloadCatImage: DownloadCatImageUseCase,
    private val notifier: SnackbarNotifier,
) : ViewModel(),
    StateOwner<CatsListState> by stateHolder {

    /**
     * Not part of [CatsListState], and cannot be: `LazyPagingItems` is built by the Composable
     * collecting this, and Paging drives loading, error and retry from there (ADR-0024).
     */
    val pagedCats: Flow<PagingData<Cat>> = getCatFeed().cachedIn(viewModelScope)

    init {
        getFavoriteCats()
            .map { favorites -> favorites.map { it.id }.toPersistentSet() }
            // Without this a throwing Room query escapes viewModelScope and kills the process.
            // No RetryableFlow: the feed renders regardless, so there is no retry button to put
            // anywhere.
            .catch { errorHandler.onFavoriteIdsFailure(it.asAppError()) }
            .onEach(stateHolder::showFavorites)
            .launchIn(viewModelScope)
    }

    override fun onCleared() = stateHolder.reset()

    fun onEvent(event: CatsListEvent) {
        when (event) {
            // A failed toggle leaves the feed intact, so it is a Snackbar, not an error state.
            is CatsListEvent.ToggleFavorite -> launchCatching(
                onFailure = { notifier.showMessage(FAVORITE_FAILED) },
            ) {
                toggleFavorite(event.cat)
            }

            is CatsListEvent.Download -> downloadCat(event.cat, downloadCatImage, notifier)
        }
    }
}
