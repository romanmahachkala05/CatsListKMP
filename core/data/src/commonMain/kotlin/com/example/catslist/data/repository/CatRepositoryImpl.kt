package com.example.catslist.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.catslist.data.error.ErrorMapper
import com.example.catslist.data.local.CatDao
import com.example.catslist.data.local.toDomain
import com.example.catslist.data.local.toEntity
import com.example.catslist.data.remote.CatApiService
import com.example.catslist.data.remote.CatFeedPagingSource
import com.example.catslist.domain.model.AppErrorException
import com.example.catslist.domain.model.Cat
import com.example.catslist.domain.repository.CatRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class CatRepositoryImpl(
    private val catDao: CatDao,
    private val catApiService: CatApiService,
    private val errorMapper: ErrorMapper,
) : CatRepository {

    override val favorites: Flow<ImmutableList<Cat>> = catDao.getAllCats()
        .map { entities -> entities.map { it.toDomain() }.toPersistentList() }
        .catch { throw AppErrorException(errorMapper.map(it)) }

    /**
     * Never carries favorite status, and never `combine()`s it in: re-running `PagingData.map`
     * on the same instance crashes with "Attempt to collect twice from pageEventFlow". The
     * overlay is applied in `CatsListScreen` at render time instead.
     */
    override val feed: Flow<PagingData<Cat>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            // Must equal pageSize: the API pages by (page, limit), so a first load of another
            // size puts every later page at the wrong offset.
            initialLoadSize = PAGE_SIZE,
            // Smaller than the pageSize default: only about two cards are visible at a time.
            prefetchDistance = PREFETCH_DISTANCE,
            enablePlaceholders = false,
        ),
        // A new source per generation: a PagingSource is single-use once invalidated.
        pagingSourceFactory = { CatFeedPagingSource(catApiService, errorMapper) },
    ).flow

    override suspend fun toggleFavorite(cat: Cat) = typed { catDao.toggleFavorite(cat.toEntity()) }

    override suspend fun removeFavorite(cat: Cat) = typed { catDao.deleteCat(cat.toEntity()) }

    /**
     * Runs [block], turning anything it throws into an [AppErrorException]. This is the layer
     * boundary: past here, a caller branches on an `AppError` and never on a Room or Retrofit
     * exception class (ADR-0028).
     */
    @Suppress("TooGenericExceptionCaught") // Catching broadly is the point, as in ADR-0013.
    private suspend fun <T> typed(block: suspend () -> T): T = try {
        block()
    } catch (error: CancellationException) {
        // The caller went away. Not a failure, and not ours to reclassify.
        throw error
    } catch (error: Exception) {
        throw AppErrorException(errorMapper.map(error))
    }

    private companion object {
        const val PAGE_SIZE = 10

        /** Roughly one screen of cards ahead of the last visible one. */
        const val PREFETCH_DISTANCE = 3
    }
}
