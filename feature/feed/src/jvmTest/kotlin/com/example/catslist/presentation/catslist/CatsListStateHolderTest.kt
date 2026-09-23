package com.example.catslist.presentation.catslist

import com.example.catslist.presentation.UiText
import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Test

class CatsListStateHolderTest {

    private val stateHolder = CatsListStateHolder()

    /** Which message it is does not matter here — the holder stores what it is handed. */
    private val unavailable = UiText.Raw("favorites are unavailable")

    @Test
    fun `starts live with no favorites`() {
        // Unlike the favorites screen, nothing here waits on the ids: the cats render either
        // way, so an empty set is a valid first answer.
        assertThat(stateHolder.state.value).isEqualTo(CatsListState())
        assertThat(stateHolder.state.value.favoritesStatus).isEqualTo(CatsListFavoritesStatus.Live)
    }

    @Test
    fun `showing favorites replaces the id set`() {
        stateHolder.showFavorites(persistentSetOf("1", "2"))

        stateHolder.showFavorites(persistentSetOf("2"))

        assertThat(stateHolder.state.value.favoriteIds).containsExactly("2")
    }

    @Test
    fun `going unavailable keeps the ids already on screen`() {
        stateHolder.showFavorites(persistentSetOf("1"))

        stateHolder.showFavoritesUnavailable(unavailable)

        val state = stateHolder.state.value
        assertThat(state.favoritesStatus).isEqualTo(CatsListFavoritesStatus.Unavailable(unavailable))
        assertThat(state.favoriteIds).containsExactly("1")
    }

    @Test
    fun `a later emission recovers from unavailable`() {
        stateHolder.showFavoritesUnavailable(unavailable)

        stateHolder.showFavorites(persistentSetOf("1"))

        assertThat(stateHolder.state.value.favoritesStatus).isEqualTo(CatsListFavoritesStatus.Live)
    }

    @Test
    fun `reset returns to the initial state`() {
        stateHolder.showFavorites(persistentSetOf("1"))
        stateHolder.showFavoritesUnavailable(unavailable)

        stateHolder.reset()

        assertThat(stateHolder.state.value).isEqualTo(CatsListState())
    }
}
