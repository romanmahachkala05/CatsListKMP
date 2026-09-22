package com.example.catslist.presentation.favoritecats

import com.example.catslist.domain.model.AppError
import com.example.catslist.feature.favorites.R
import com.example.catslist.presentation.UiText
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FavoriteCatsErrorHandlerTest {

    private val stateHolder = FavoriteCatsStateHolder()
    private val errorHandler = FavoriteCatsErrorHandler(stateHolder)

    @Test
    fun `a broken favorites stream becomes an error on screen`() {
        errorHandler.onFavoritesFailure(AppError.Storage)

        assertThat(stateHolder.state.value.status).isEqualTo(
            FavoriteCatsUiStatus.Error(UiText.Resource(R.string.favoritecats_error_loading_favorites)),
        )
    }
}
