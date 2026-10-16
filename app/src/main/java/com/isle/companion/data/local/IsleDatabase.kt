package com.isle.companion.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.isle.companion.data.local.dao.CharacterDao
import com.isle.companion.data.local.dao.MemoryDao
import com.isle.companion.data.local.dao.MessageDao
import com.isle.companion.data.local.entity.CharacterEntity
import com.isle.companion.data.local.entity.MemoryEntity
import com.isle.companion.data.local.entity.MessageEntity

@Database(
    entities = [
        CharacterEntity::class,
        MessageEntity::class,
        MemoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class IsleDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
    abstract fun messageDao(): MessageDao
    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile
        private var INSTANCE: IsleDatabase? = null

        fun get(context: Context): IsleDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    IsleDatabase::class.java,
                    "isle.db"
                ).build().also { INSTANCE = it }
            }
    }
}
