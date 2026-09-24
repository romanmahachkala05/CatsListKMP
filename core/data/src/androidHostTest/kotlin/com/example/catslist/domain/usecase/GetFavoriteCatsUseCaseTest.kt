package com.example.catslist.domain.usecase

import com.example.catslist.testing.FakeCatRepository
import com.example.catslist.testing.cat
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GetFavoriteCatsUseCaseTest {

    private val repository = FakeCatRepository()
    private val getFavoriteCats = GetFavoriteCatsUseCase(repository)

    @Test
    fun `emits nothing when no cat is favorited`() = runTest {
        assertThat(getFavoriteCats().first()).isEmpty()
    }

    @Test
    fun `emits the favorited cats`() = runTest {
        repository.setFavorites(cat("1"), cat("2"))

        assertThat(getFavoriteCats().first().map { it.id }).containsExactly("1", "2")
    }
}
