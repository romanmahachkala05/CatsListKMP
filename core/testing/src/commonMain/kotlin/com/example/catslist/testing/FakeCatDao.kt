package com.example.catslist.testing

import com.example.catslist.data.local.CatDao
import com.example.catslist.data.local.CatEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory stand-in for the Room DAO. Extends [CatDao] rather than reimplementing it, so the
 * `toggleFavorite` under test is the body Room runs. It cannot reproduce `@Transaction`,
 * though: the block runs inline, proving the logic and not the atomicity.
 */
class FakeCatDao : CatDao() {

    private val rows = MutableStateFlow<List<CatEntity>>(emptyList())

    override fun getAllCats(): Flow<List<CatEntity>> = rows.asStateFlow()

    /** Rejects a duplicate id as the real `@Insert` does; `SQLiteConstraintException` is
     * not available off-device. */
    override suspend fun insertCat(cat: CatEntity) {
        check(rows.value.none { it.id == cat.id }) { "UNIQUE constraint failed: favoriteCatsTable.id (${cat.id})" }
        rows.value = rows.value + cat
    }

    override suspend fun deleteCat(cat: CatEntity) {
        rows.value = rows.value.filterNot { it.id == cat.id }
    }

    override suspend fun isFavorite(id: String): Boolean = rows.value.any { it.id == id }

    override suspend fun deleteAllCats() {
        rows.value = emptyList()
    }
}
