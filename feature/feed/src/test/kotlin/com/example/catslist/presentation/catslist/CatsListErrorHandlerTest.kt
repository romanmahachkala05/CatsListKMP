package com.example.catslist.presentation.catslist

import com.example.catslist.domain.model.AppError
import com.example.catslist.feature.feed.R
import com.example.catslist.presentation.UiText
import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentSetOf
import org.junit.Test

class CatsListErrorHandlerTest {

    private val stateHolder = CatsListStateHolder()
    private val errorHandler = CatsListErrorHandler(stateHolder)

    @Test
    fun `a broken favorites stream marks the overlay unavailable`() {
        errorHandler.onFavoriteIdsFailure(AppError.Storage)

        assertThat(stateHolder.state.value.favoritesStatus).isEqualTo(
            CatsListFavoritesStatus.Unavailable(UiText.Resource(R.string.catslist_error_favorites_unavailable)),
        )
    }

    @Test
    fun `the failure does not clear the ids the feed is already rendering`() {
        stateHolder.showFavorites(persistentSetOf("1"))

        errorHandler.onFavoriteIdsFailure(AppError.Storage)

        assertThat(stateHolder.state.value.favoriteIds).containsExactly("1")
    }
}
