package com.example.catslist.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

/**
 * Desktop has no per-app sandbox, so the file goes under the user's home in a directory named
 * for the app rather than loose in the working directory.
 *
 * [directory] is a parameter so a test can point the same builder at a temporary path.
 */
fun catDatabaseBuilder(
    directory: File = File(System.getProperty("user.home"), ".catslist"),
    name: String = CAT_DATABASE_NAME,
): RoomDatabase.Builder<CatDatabase> {
    directory.mkdirs()
    return Room.databaseBuilder<CatDatabase>(name = File(directory, name).absolutePath)
        .withCatDatabaseDefaults()
}
