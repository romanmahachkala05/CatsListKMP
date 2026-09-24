package com.example.catslist.data.local

import com.example.catslist.domain.model.Cat
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID

/**
 * `CatDatabaseJvmTest`'s iOS twin: the real database, built through the same
 * `withCatDatabaseDefaults()` the app ships, on Kotlin/Native. Room's generated constructor
 * and the bundled SQLite are per-target, so passing on the JVM says nothing about this one.
 */
@OptIn(ExperimentalForeignApi::class)
class CatDatabaseIosTest {

    private val directory = NSTemporaryDirectory() + NSUUID().UUIDString
    private val database by lazy {
        NSFileManager.defaultManager.createDirectoryAtPath(directory, true, null, null)
        catDatabaseBuilder(directory = directory).build()
    }

    @AfterTest
    fun tearDown() {
        database.close()
        NSFileManager.defaultManager.removeItemAtPath(directory, null)
    }

    @Test
    fun persistsAFavoriteAndReadsItBack() = runTest {
        database.catDao().toggleFavorite(cat("1").toEntity())

        assertEquals(listOf("1"), database.catDao().getAllCats().first().map { it.id })
    }

    @Test
    fun togglingTheSameCatTwiceRemovesIt() = runTest {
        val dao = database.catDao()
        dao.toggleFavorite(cat("1").toEntity())

        dao.toggleFavorite(cat("1").toEntity())

        assertTrue(dao.getAllCats().first().isEmpty())
    }

    @Test
    fun keepsEveryColumnAcrossTheRoundTrip() = runTest {
        database.catDao().toggleFavorite(cat("1").toEntity())

        val stored = database.catDao().getAllCats().first().single()

        assertEquals(CatEntity(id = "1", url = "https://cdn/1.jpg", width = 300, height = 200), stored)
    }

    private fun cat(id: String) = Cat(id = id, url = "https://cdn/$id.jpg", width = 300, height = 200)
}
