package com.example.catslist.data.local

import com.example.catslist.domain.model.Cat
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * The desktop half of the multiplatform claim, checked rather than asserted: this builds the
 * *real* database through the same `withCatDatabaseDefaults()` the app ships, on the JVM,
 * with no Android on the classpath (ADR-0029).
 *
 * If Room's KMP codegen, the bundled SQLite driver or the migration set were wrong for this
 * target, nothing else in the build would say so.
 */
class CatDatabaseJvmTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val database by lazy { catDatabaseBuilder(directory = temporaryFolder.root).build() }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `persists a favorite and reads it back`() {
        runBlocking {
            database.catDao().toggleFavorite(cat("1").toEntity())

            val favorites = database.catDao().getAllCats().first()

            assertThat(favorites.map { it.id }).containsExactly("1")
        }
    }

    @Test
    fun `toggling the same cat twice removes it`() {
        runBlocking {
            val dao = database.catDao()
            dao.toggleFavorite(cat("1").toEntity())

            dao.toggleFavorite(cat("1").toEntity())

            assertThat(dao.getAllCats().first()).isEmpty()
        }
    }

    @Test
    fun `keeps every column across the round trip`() {
        runBlocking {
            database.catDao().toggleFavorite(cat("1").toEntity())

            val stored = database.catDao().getAllCats().first().single()

            assertThat(stored).isEqualTo(CatEntity(id = "1", url = "https://cdn/1.jpg", width = 300, height = 200))
        }
    }

    private fun cat(id: String) = Cat(id = id, url = "https://cdn/$id.jpg", width = 300, height = 200)
}
