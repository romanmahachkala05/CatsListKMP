package com.example.catslist.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** An abstract class, not an interface, so [toggleFavorite] can carry a body Room wraps. */
@Dao
abstract class CatDao {

    @Insert
    abstract suspend fun insertCat(cat: CatEntity)

    @Delete
    abstract suspend fun deleteCat(cat: CatEntity)

    @Query("SELECT * FROM favoriteCatsTable")
    abstract fun getAllCats(): Flow<List<CatEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favoriteCatsTable WHERE id = :id)")
    abstract suspend fun isFavorite(id: String): Boolean

    @Query("DELETE FROM favoriteCatsTable")
    abstract suspend fun deleteAllCats()

    /**
     * Adds or removes [cat] in one transaction. The read and the write have to be atomic:
     * two quick taps could otherwise both read "not a favorite" and insert twice, which
     * [insertCat] aborts on. `@Transaction` queues concurrent calls instead of interleaving.
     */
    @Transaction
    open suspend fun toggleFavorite(cat: CatEntity) {
        if (isFavorite(cat.id)) deleteCat(cat) else insertCat(cat)
    }
}
