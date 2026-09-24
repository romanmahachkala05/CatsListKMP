package com.example.catslist.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(
    entities = [CatEntity::class],
    version = 5,
    exportSchema = true,
)
@ConstructedBy(CatDatabaseConstructor::class)
abstract class CatDatabase : RoomDatabase() {
    abstract fun catDao(): CatDao
}

/**
 * Room's KSP processor writes the `actual` for every target. There is no hand-written one,
 * which is exactly what the suppression says — the IDE cannot see generated actuals.
 */
@Suppress("KotlinNoActualForExpect", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object CatDatabaseConstructor : RoomDatabaseConstructor<CatDatabase> {
    override fun initialize(): CatDatabase
}
