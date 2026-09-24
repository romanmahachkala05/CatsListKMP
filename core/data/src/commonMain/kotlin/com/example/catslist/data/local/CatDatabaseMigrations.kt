package com.example.catslist.data.local

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * v2 -> v3: drop the redundant `favorite` column. The SQLite this targets has no
 * ALTER TABLE ... DROP COLUMN, so the table is recreated instead.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS favoriteCatsTable_new (
                id TEXT NOT NULL PRIMARY KEY,
                url TEXT NOT NULL,
                width INTEGER NOT NULL,
                height INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        connection.execSQL(
            """
            INSERT INTO favoriteCatsTable_new (id, url, width, height)
            SELECT id, url, width, height FROM favoriteCatsTable
            """.trimIndent(),
        )
        connection.execSQL("DROP TABLE favoriteCatsTable")
        connection.execSQL("ALTER TABLE favoriteCatsTable_new RENAME TO favoriteCatsTable")
    }
}

/**
 * v3 -> v4: add the Paging 3 feed cache. Kept although [MIGRATION_4_5] drops both tables
 * again — a v3 install still has to reach v5, one step at a time.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS feedCatsTable (
                id TEXT NOT NULL PRIMARY KEY,
                url TEXT NOT NULL,
                width INTEGER NOT NULL,
                height INTEGER NOT NULL,
                sortOrder INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS feedRemoteKeysTable (
                id INTEGER NOT NULL PRIMARY KEY,
                nextPage INTEGER
            )
            """.trimIndent(),
        )
    }
}

/**
 * v4 -> v5: drop the feed cache. The feed pages straight from the network now, and only
 * favorites are kept on disk. No user data is lost; favoriteCatsTable is untouched.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS feedCatsTable")
        connection.execSQL("DROP TABLE IF EXISTS feedRemoteKeysTable")
    }
}
