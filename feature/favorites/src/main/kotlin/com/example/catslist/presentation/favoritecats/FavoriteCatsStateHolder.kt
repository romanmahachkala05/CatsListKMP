package com.example.catslist.presentation.favoritecats

import com.example.catslist.domain.model.Cat
import com.example.catslist.presentation.StateOwner
import com.example.catslist.presentation.UiText
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal interface IFavoriteCatsStateHolder : StateOwner<FavoriteCatsState> {
    fun showFavorites(cats: ImmutableList<Cat>)
    fun showLoading()
    fun showError(message: UiText)
    fun reset()
}

internal class FavoriteCatsStateHolder : IFavoriteCatsStateHolder {

    private val _state = MutableStateFlow(FavoriteCatsState())
    override val state: StateFlow<FavoriteCatsState> = _state.asStateFlow()

    override fun showFavorites(cats: ImmutableList<Cat>) = _state.update {
        it.copy(
            status = if (cats.isEmpty()) FavoriteCatsUiStatus.Empty else FavoriteCatsUiStatus.Content,
            cats = cats,
        )
    }

    /** Keeps the cats already on screen: a retry redisplays them rather than starting blank. */
    override fun showLoading() = _state.update {
        it.copy(status = FavoriteCatsUiStatus.Loading)
    }

    override fun showError(message: UiText) = _state.update {
        it.copy(status = FavoriteCatsUiStatus.Error(message))
    }

    override fun reset() = _state.update { FavoriteCatsState() }
}
