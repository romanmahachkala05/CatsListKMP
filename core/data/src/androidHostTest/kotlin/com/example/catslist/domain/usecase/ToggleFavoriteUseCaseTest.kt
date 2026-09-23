package com.example.catslist.domain.usecase

import com.example.catslist.testing.FakeCatRepository
import com.example.catslist.testing.cat
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ToggleFavoriteUseCaseTest {

    private val repository = FakeCatRepository()
    private val toggleFavorite = ToggleFavoriteUseCase(repository)

    @Test
    fun `favorites a cat that is not favorited yet`() = runTest {
        toggleFavorite(cat("1"))

        assertThat(repository.favorites.first().map { it.id }).containsExactly("1")
    }

    @Test
    fun `unfavorites a cat that is already favorited`() = runTest {
        repository.setFavorites(cat("1"))

        toggleFavorite(cat("1"))

        assertThat(repository.favorites.first()).isEmpty()
    }
}
