package com.example.catslist.data.repository

import androidx.paging.testing.asSnapshot
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.catslist.data.error.ErrorMapper
import com.example.catslist.data.local.CatDatabase
import com.example.catslist.data.remote.CatApiService
import com.example.catslist.data.remote.CatDto
import com.example.catslist.domain.model.Cat
import com.example.catslist.testing.FakeNetworkMonitor
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs the real `Pager` that `CatRepositoryImpl.feed` builds, which a plain JVM `runTest` does
 * not exercise reliably. Room is real here for `favorites`; the feed never touches it.
 */
@RunWith(AndroidJUnit4::class)
class CatRepositoryImplTest {

    private lateinit var database: CatDatabase
    private lateinit var repository: CatRepositoryImpl
    private lateinit var api: FakeApi

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, CatDatabase::class.java).build()
        api = FakeApi()
        repository = CatRepositoryImpl(database.catDao(), api, ErrorMapper(FakeNetworkMonitor()))
    }

    @After
    fun tearDown() = database.close()

    /** `feed` never carries favorite status (ADR-0024); `false` here is the contract. */
    @Test
    fun feedNeverCarriesFavoriteStatus() = runBlocking {
        api.enqueue(dto("1"))
        repository.toggleFavorite(cat("1"))

        val snapshot = repository.feed.asSnapshot()

        assertEquals(false, snapshot.single().isFavorite)
    }

    /**
     * The regression this guards against (ADR-0024): a favoriting-invalidates-the-feed query
     * wiped every loaded page, and a `combine()` re-mapping the same `PagingData` crashed with
     * "Attempt to collect twice from pageEventFlow".
     */
    @Test
    fun favoritingDoesNotResetAlreadyLoadedPages() = runBlocking {
        api.enqueue(dto("1"))
        api.enqueue(dto("2"))

        // Both calls in one collection: `feed` is a single Pager, and a second independent
        // `asSnapshot` collection against it is not what it is for.
        val snapshot = repository.feed.asSnapshot {
            appendScrollWhile { it.id != "2" }
            repository.toggleFavorite(cat("1"))
        }

        assertEquals(
            "a favorite toggle must not wipe cats already loaded",
            listOf("1", "2"),
            snapshot.map { it.id },
        )
    }

    private fun dto(id: String) = CatDto(id = id, url = "https://cdn.example/$id.jpg", width = 300, height = 200)

    private fun cat(id: String) =
        Cat(id = id, url = "https://cdn.example/$id.jpg", width = 300, height = 200, isFavorite = false)

    private class FakeApi : CatApiService {
        private val responses = ArrayDeque<List<CatDto>>()
        fun enqueue(vararg cats: CatDto) = responses.addLast(cats.toList())
        override suspend fun requestCatInfo(limit: Int, page: Int): List<CatDto> =
            responses.removeFirstOrNull().orEmpty()
    }
}
