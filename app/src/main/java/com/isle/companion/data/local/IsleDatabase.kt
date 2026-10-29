package com.isle.companion.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
    exportSchema = false
)
abstract class IsleDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
    abstract fun messageDao(): MessageDao
    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile
        private var INSTANCE: IsleDatabase? = null

        /** v1 → v2：为按角色 + 时间排序补复合索引。 */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_messages_characterId_timestamp` " +
                        "ON `messages` (`characterId`, `timestamp`)"
                )
            }
        }

        fun get(context: Context): IsleDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    IsleDatabase::class.java,
                    "isle.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
