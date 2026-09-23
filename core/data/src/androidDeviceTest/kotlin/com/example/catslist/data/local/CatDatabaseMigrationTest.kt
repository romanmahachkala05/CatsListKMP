package com.example.catslist.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises [MIGRATION_2_3] against a real database built from the committed v2 schema. An
 * untested migration takes the user's favorites with it the first time it is wrong.
 */
@RunWith(AndroidJUnit4::class)
class CatDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        CatDatabase::class.java,
    )

    @Test
    fun migrate2To3_keepsEveryFavorite() {
        helper.createDatabase(TEST_DB, 2).use { db ->
            db.execSQL(insertV2(id = "1", favorite = 1))
            db.execSQL(insertV2(id = "2", favorite = 1))
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)

        migrated.query("SELECT id FROM favoriteCatsTable ORDER BY id").use { cursor ->
            assertEquals(2, cursor.count)
            cursor.moveToFirst()
            assertEquals("1", cursor.getString(0))
            cursor.moveToNext()
            assertEquals("2", cursor.getString(0))
        }
    }

    @Test
    fun migrate2To3_carriesEveryColumnAcrossTheRebuild() {
        // The migration recreates the table, so a column left out of the INSERT would end up
        // null or default rather than failing.
        helper.createDatabase(TEST_DB, 2).use { db ->
            db.execSQL(insertV2(id = "1", favorite = 1))
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)

        migrated.query("SELECT id, url, width, height FROM favoriteCatsTable").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("1", cursor.getString(0))
            assertEquals("https://cdn.example/1.jpg", cursor.getString(1))
            assertEquals(300, cursor.getInt(2))
            assertEquals(200, cursor.getInt(3))
        }
    }

    @Test
    fun migrate2To3_dropsTheRedundantFavoriteColumn() {
        helper.createDatabase(TEST_DB, 2).use { db ->
            db.execSQL(insertV2(id = "1", favorite = 1))
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)

        migrated.query("PRAGMA table_info(favoriteCatsTable)").use { cursor ->
            val columns = buildList {
                while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
            }
            assertEquals(listOf("id", "url", "width", "height"), columns)
            assertFalse("favorite" in columns)
        }
    }

    @Test
    fun migrate2To3_survivesAnEmptyTable() {
        helper.createDatabase(TEST_DB, 2).close()

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)

        migrated.query("SELECT COUNT(*) FROM favoriteCatsTable").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }

    @Test
    fun migrate3To4_createsTheEmptyFeedCacheTables() {
        helper.createDatabase(TEST_DB, 3).use { db ->
            db.execSQL(insertV3(id = "1"))
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4)

        migrated.query("SELECT COUNT(*) FROM feedCatsTable").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        migrated.query("SELECT COUNT(*) FROM feedRemoteKeysTable").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        // Nothing about the existing favorites table is touched by an unrelated migration.
        migrated.query("SELECT id FROM favoriteCatsTable").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("1", cursor.getString(0))
        }
    }

    private fun insertV2(id: String, favorite: Int) = """
        INSERT INTO favoriteCatsTable (id, url, width, height, favorite)
        VALUES ('$id', 'https://cdn.example/$id.jpg', 300, 200, $favorite)
    """.trimIndent()

    private fun insertV3(id: String) = """
        INSERT INTO favoriteCatsTable (id, url, width, height)
        VALUES ('$id', 'https://cdn.example/$id.jpg', 300, 200)
    """.trimIndent()

    private companion object {
        const val TEST_DB = "migration-test-db"
    }
}
