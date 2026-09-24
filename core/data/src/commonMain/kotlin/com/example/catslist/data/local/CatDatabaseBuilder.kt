package com.example.catslist.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/** The production name. A test passes its own so it doesn't touch the real database. */
const val CAT_DATABASE_NAME = "cats_database"

/**
 * Everything about the database that is the same on every platform. Only *where the file
 * lives* differs, which is what the platform builder supplies.
 *
 * One place the database is configured, so tests exercise the build the app ships.
 */
fun RoomDatabase.Builder<CatDatabase>.withCatDatabaseDefaults(): RoomDatabase.Builder<CatDatabase> =
    addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
        // v1 shipped a different table and was only ever wiped, never migrated. Scoping the
        // fallback to that one version keeps a v1 install from crashing on launch while any
        // other missing migration still fails loudly.
        .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1)
        // KMP Room has no default driver: the bundled one ships its own SQLite, so every
        // platform gets the same engine rather than whatever the host happens to provide.
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
