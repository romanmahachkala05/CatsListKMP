package com.example.catslist.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

/** Android puts the file in the app's private database directory, by name. */
fun catDatabaseBuilder(context: Context, name: String = CAT_DATABASE_NAME): RoomDatabase.Builder<CatDatabase> =
    Room.databaseBuilder<CatDatabase>(
        context = context.applicationContext,
        name = context.getDatabasePath(name).absolutePath,
    ).withCatDatabaseDefaults()
