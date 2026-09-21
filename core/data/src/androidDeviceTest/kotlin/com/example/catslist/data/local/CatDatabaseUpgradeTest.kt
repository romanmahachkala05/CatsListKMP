package com.example.catslist.data.local

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Opens an older database through [catDatabaseBuilder], so the upgrade path is the real one. */
@RunWith(AndroidJUnit4::class)
class CatDatabaseUpgradeTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() = deleteTestDatabase()

    @After
    fun tearDown() = deleteTestDatabase()

    /**
     * v1 shipped a different table and was only ever wiped, never migrated. Registering
     * MIGRATION_2_3 alone turned that into a crash on launch.
     */
    @Test
    fun aVersion1DatabaseIsRecreatedInsteadOfCrashing() {
        writeVersion1Database()

        val database = catDatabaseBuilder(context, TEST_DB).build()
        val favorites = runBlocking { database.catDao().getAllCats().first() }
        database.close()

        assertTrue("a v1 database is wiped, as the original app did", favorites.isEmpty())
    }

    @Test
    fun aVersion2DatabaseKeepsItsFavorites() {
        // The contrast: v2 has a migration, so it is upgraded rather than wiped.
        writeVersion2Database()

        val database = catDatabaseBuilder(context, TEST_DB).build()
        val favorites = runBlocking { database.catDao().getAllCats().first() }
        database.close()

        assertEquals(listOf("1"), favorites.map { it.id })
    }

    private fun writeVersion1Database() = context
        .openOrCreateDatabase(TEST_DB, Context.MODE_PRIVATE, null)
        .use { db ->
            db.execSQL(
                """
                CREATE TABLE favoriteCats (
                    id TEXT NOT NULL PRIMARY KEY,
                    url TEXT NOT NULL,
                    width INTEGER NOT NULL,
                    height INTEGER NOT NULL,
                    favourite INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL("INSERT INTO favoriteCats VALUES ('1', 'https://cdn.example/1.jpg', 300, 200, 1)")
            db.version = 1
        }

    private fun writeVersion2Database() = context
        .openOrCreateDatabase(TEST_DB, Context.MODE_PRIVATE, null)
        .use { db ->
            db.execSQL(
                """
                CREATE TABLE favoriteCatsTable (
                    id TEXT NOT NULL PRIMARY KEY,
                    url TEXT NOT NULL,
                    width INTEGER NOT NULL,
                    height INTEGER NOT NULL,
                    favorite INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL("INSERT INTO favoriteCatsTable VALUES ('1', 'https://cdn.example/1.jpg', 300, 200, 1)")
            db.version = 2
        }

    private fun deleteTestDatabase() {
        context.deleteDatabase(TEST_DB)
    }

    private companion object {
        const val TEST_DB = "upgrade-test-db"
    }
}
