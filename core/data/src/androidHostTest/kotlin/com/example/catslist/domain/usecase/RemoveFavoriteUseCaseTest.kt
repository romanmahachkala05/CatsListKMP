package com.example.catslist.domain.usecase

import com.example.catslist.testing.FakeCatRepository
import com.example.catslist.testing.cat
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RemoveFavoriteUseCaseTest {

    private val repository = FakeCatRepository()
    private val removeFavorite = RemoveFavoriteUseCase(repository)

    @Test
    fun `removes only the given cat`() = runTest {
        repository.setFavorites(cat("1"), cat("2"))

        removeFavorite(cat("1"))

        assertThat(repository.favorites.first().map { it.id }).containsExactly("2")
    }

    @Test
    fun `leaves favorites untouched when the cat is not among them`() = runTest {
        repository.setFavorites(cat("1"))

        removeFavorite(cat("2"))

        assertThat(repository.favorites.first().map { it.id }).containsExactly("1")
    }
}
