package com.example.catslist.data.remote

import androidx.paging.PagingSource
import com.example.catslist.data.error.ErrorMapper
import com.example.catslist.domain.model.AppError
import com.example.catslist.domain.model.AppErrorException
import com.example.catslist.testing.FakeCatApiService
import com.example.catslist.testing.FakeNetworkMonitor
import com.example.catslist.testing.catDto
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import org.junit.Test

class CatFeedPagingSourceTest {

    private val api = FakeCatApiService()
    private val networkMonitor = FakeNetworkMonitor()
    private val pagingSource = CatFeedPagingSource(api, ErrorMapper(networkMonitor))

    @Test
    fun `the first load asks for page 0 and points at the next one`() = runTest {
        api.enqueueResponse(catDto("1"), catDto("2"))

        val page = pagingSource.refresh() as PagingSource.LoadResult.Page

        assertThat(api.lastRequestedPage).isEqualTo(0)
        assertThat(page.data.map { it.id }).containsExactly("1", "2").inOrder()
        assertThat(page.nextKey).isEqualTo(1)
    }

    @Test
    fun `the feed only ever appends`() = runTest {
        api.enqueueResponse(catDto("1"))

        val page = pagingSource.refresh() as PagingSource.LoadResult.Page

        // Nothing is ever above the first page, so Paging must never be told to look.
        assertThat(page.prevKey).isNull()
    }

    @Test
    fun `an empty response ends pagination`() = runTest {
        api.enqueueResponse()

        val page = pagingSource.refresh() as PagingSource.LoadResult.Page

        assertThat(page.nextKey).isNull()
    }

    @Test
    fun `duplicates within one response are dropped`() = runTest {
        // TheCatAPI's search endpoint can hand back the same cat twice in one response.
        api.enqueueResponse(catDto("1"), catDto("1"), catDto("2"))

        val page = pagingSource.refresh() as PagingSource.LoadResult.Page

        assertThat(page.data.map { it.id }).containsExactly("1", "2").inOrder()
    }

    @Test
    fun `a cat already sent is not sent again by a later page`() = runTest {
        // The list keys its items by id, so a repeat across pages is a crash (ADR-0015).
        api.enqueueResponse(catDto("1"), catDto("2"))
        api.enqueueResponse(catDto("2"), catDto("3"))

        val first = pagingSource.refresh() as PagingSource.LoadResult.Page
        val second = pagingSource.append(first.nextKey) as PagingSource.LoadResult.Page

        assertThat(second.data.map { it.id }).containsExactly("3")
    }

    @Test
    fun `a page of nothing but repeats still leads to the next page`() = runTest {
        api.enqueueResponse(catDto("1"))
        api.enqueueResponse(catDto("1"))

        val first = pagingSource.refresh() as PagingSource.LoadResult.Page
        val second = pagingSource.append(first.nextKey) as PagingSource.LoadResult.Page

        // Emptied by de-duplication, not by the API running out: stopping here would strand
        // the feed on a page the server has more behind.
        assertThat(second.data).isEmpty()
        assertThat(second.nextKey).isEqualTo(2)
    }

    @Test
    fun `a network failure becomes a load error rather than an exception`() = runTest {
        api.error = IOException("offline")

        val result = pagingSource.refresh()

        assertThat(result).isInstanceOf(PagingSource.LoadResult.Error::class.java)
    }

    @Test
    fun `a malformed response becomes a load error too`() = runTest {
        // Not every failure is an IOException: the converter throws this for a 200 whose body
        // is not the JSON the wire model expects, and it would otherwise escape `load()`.
        api.error = SerializationException("Unexpected JSON token")

        val result = pagingSource.refresh()

        assertThat(result).isInstanceOf(PagingSource.LoadResult.Error::class.java)
    }

    @Test
    fun `the error a load carries is classified, not raw`() = runTest {
        // What the screen reads out of loadState. A raw IOException there would put the
        // Unknown fallback on a feed that is simply offline (ADR-0028).
        networkMonitor.setOnline(false)
        api.error = IOException("offline")

        val result = pagingSource.refresh() as PagingSource.LoadResult.Error

        assertThat((result.throwable as AppErrorException).error).isEqualTo(AppError.NoConnection)
    }

    @Test
    fun `the same failure is Unreachable while the device is online`() = runTest {
        // Same exception, different advice: telling a connected user to check their connection
        // sends them to fix something that is not broken.
        networkMonitor.setOnline(true)
        api.error = IOException("connection reset")

        val result = pagingSource.refresh() as PagingSource.LoadResult.Error

        assertThat((result.throwable as AppErrorException).error).isEqualTo(AppError.Unreachable)
    }

    @Test
    fun `a canceled load is rethrown rather than reported as an error`() = runTest {
        // Paging cancels the loads it no longer needs; reporting those would show the user an
        // error for scrolling away, and would break structured concurrency.
        api.error = CancellationException("no longer needed")

        val thrown = runCatching { pagingSource.refresh() }.exceptionOrNull()

        assertThat(thrown).isInstanceOf(CancellationException::class.java)
    }

    private suspend fun CatFeedPagingSource.refresh() =
        load(PagingSource.LoadParams.Refresh(key = null, loadSize = LOAD_SIZE, placeholdersEnabled = false))

    private suspend fun CatFeedPagingSource.append(key: Int?) = load(
        PagingSource.LoadParams.Append(
            key = requireNotNull(key),
            loadSize = LOAD_SIZE,
            placeholdersEnabled = false,
        ),
    )

    private companion object {
        const val LOAD_SIZE = 10
    }
}
