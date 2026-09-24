package com.example.catslist.domain.usecase

import androidx.paging.testing.asSnapshot
import com.example.catslist.testing.FakeCatRepository
import com.example.catslist.testing.cat
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GetCatFeedUseCaseTest {

    private val repository = FakeCatRepository()
    private val getCatFeed = GetCatFeedUseCase(repository)

    @Test
    fun `emits the repository feed`() = runTest {
        repository.setFeed(cat("1"), cat("2"))

        val feed = getCatFeed().asSnapshot()

        assertThat(feed.map { it.id }).containsExactly("1", "2").inOrder()
    }
}
