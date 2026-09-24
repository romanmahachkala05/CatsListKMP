package com.example.catslist.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Runs against a real in-memory Room database: `@Transaction` is Room's, and no fake has it. */
@RunWith(AndroidJUnit4::class)
class CatDaoTest {

    private lateinit var database: CatDatabase
    private lateinit var dao: CatDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, CatDatabase::class.java).build()
        dao = database.catDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun toggleFavorite_storesACatThatIsNotStoredYet() = runBlocking {
        dao.toggleFavorite(entity("1"))

        assertEquals(listOf("1"), dao.getAllCats().first().map { it.id })
    }

    @Test
    fun toggleFavorite_removesACatThatIsAlreadyStored() = runBlocking {
        dao.toggleFavorite(entity("1"))

        dao.toggleFavorite(entity("1"))

        assertTrue(dao.getAllCats().first().isEmpty())
    }

    @Test
    fun insertCat_rejectsADuplicateId() = runBlocking {
        dao.insertCat(entity("1"))

        runCatching { dao.insertCat(entity("1")) }.let { result ->
            assertTrue("expected the primary key to abort a duplicate insert", result.isFailure)
        }
    }

    /**
     * The regression test for the double-tap crash: unserialized, concurrent callers could
     * each read "not a favorite" and both insert. An even number of toggles has to be a no-op.
     */
    @Test
    fun toggleFavorite_survivesConcurrentCallersAndEndsConsistent() = runBlocking {
        val cat = entity("1")

        withContext(Dispatchers.Default) {
            List(CONCURRENT_TOGGLES) { async { dao.toggleFavorite(cat) } }.awaitAll()
        }

        assertTrue(
            "an even number of serialized toggles must cancel out",
            dao.getAllCats().first().isEmpty(),
        )
    }

    private fun entity(id: String) = CatEntity(
        id = id,
        url = "https://cdn.example/$id.jpg",
        width = 300,
        height = 200,
    )

    private companion object {
        /** Even, so the toggles cancel out, and large enough to make interleaving likely. */
        const val CONCURRENT_TOGGLES = 50
    }
}
