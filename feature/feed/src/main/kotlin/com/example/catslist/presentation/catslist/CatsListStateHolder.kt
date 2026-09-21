package com.example.catslist.presentation.catslist

import com.example.catslist.presentation.StateOwner
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal interface ICatsListStateHolder : StateOwner<CatsListState> {
    fun showFavorites(ids: ImmutableSet<String>)
    fun showFavoritesUnavailable()
    fun reset()
}

internal class CatsListStateHolder : ICatsListStateHolder {

    private val _state = MutableStateFlow(CatsListState())
    override val state: StateFlow<CatsListState> = _state.asStateFlow()

    override fun showFavorites(ids: ImmutableSet<String>) = _state.update {
        it.copy(favoritesStatus = CatsListFavoritesStatus.Live, favoriteIds = ids)
    }

    /** Keeps the ids already on screen: the stars stop updating rather than all switching off. */
    override fun showFavoritesUnavailable() = _state.update {
        it.copy(favoritesStatus = CatsListFavoritesStatus.Unavailable)
    }

    override fun reset() = _state.update { CatsListState() }
}
