package com.example.catslist.data.remote

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.catslist.data.error.ErrorMapper
import com.example.catslist.domain.model.AppErrorException
import com.example.catslist.domain.model.Cat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Pages the feed straight from TheCatAPI, keeping nothing on disk. Only favorites persist. */
class CatFeedPagingSource(
    private val catApiService: CatApiService,
    private val errorMapper: ErrorMapper,
) : PagingSource<Int, Cat>() {

    /**
     * Ids this generation has already handed out. TheCatAPI repeats cats within and across
     * pages, and the list keys its items by id, so a repeat is a crash (ADR-0015). Per
     * generation is the right scope: a refresh builds a new source and may repeat itself.
     */
    private val seenIds = mutableSetOf<String>()

    /**
     * Paging can have a refresh and an append in flight at once, so the set needs guarding.
     * A `Mutex` rather than a concurrent collection: `java.util.concurrent` is not on every
     * platform, and the critical section is one filter over one page.
     */
    private val seenIdsLock = Mutex()

    /** Always restarts at the first page: random cats have no stable position to return to. */
    override fun getRefreshKey(state: PagingState<Int, Cat>): Int? = null

    /**
     * Every failure becomes a [LoadResult.Error]: anything thrown out of here reaches
     * `viewModelScope` through `cachedIn` and kills the process (ADR-0013). The error it
     * carries is always an [AppErrorException], so the screen reads a classified failure out
     * of `loadState` rather than guessing at an exception class (ADR-0028).
     */
    @Suppress("TooGenericExceptionCaught")
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Cat> {
        val page = params.key ?: STARTING_PAGE
        return try {
            val cats = catApiService.requestCatInfo(limit = params.loadSize, page = page)
            val fresh = seenIdsLock.withLock { cats.filter { seenIds.add(it.id) } }
            LoadResult.Page(
                data = fresh.map { it.toDomain() },
                // The feed only appends; the API has no notion of newer cats.
                prevKey = null,
                // Keyed off the response, not the filtered list: an all-repeats page still has
                // more pages behind it.
                nextKey = if (cats.isEmpty()) null else page + 1,
            )
        } catch (error: CancellationException) {
            // Paging cancels a load it no longer needs; that is not a failure to report.
            throw error
        } catch (error: Exception) {
            LoadResult.Error(AppErrorException(errorMapper.map(error)))
        }
    }

    private companion object {
        const val STARTING_PAGE = 0
    }
}
