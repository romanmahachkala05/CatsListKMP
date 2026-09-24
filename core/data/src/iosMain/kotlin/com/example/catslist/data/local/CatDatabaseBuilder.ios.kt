package com.example.catslist.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/**
 * Application Support, not Documents: it is where iOS expects data the app owns and the user
 * never browses, and it is backed up the same way.
 *
 * [directory] is a parameter so a test can point the same builder at a temporary path.
 */
fun catDatabaseBuilder(
    directory: String = applicationSupportDirectory(),
    name: String = CAT_DATABASE_NAME,
): RoomDatabase.Builder<CatDatabase> =
    Room.databaseBuilder<CatDatabase>(name = "$directory/$name").withCatDatabaseDefaults()

@OptIn(ExperimentalForeignApi::class)
private fun applicationSupportDirectory(): String {
    // `create = true`: unlike Documents, this directory does not exist until someone asks.
    val url = NSFileManager.defaultManager.URLForDirectory(
        directory = NSApplicationSupportDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    return requireNotNull(url?.path) { "No Application Support directory" }
}
