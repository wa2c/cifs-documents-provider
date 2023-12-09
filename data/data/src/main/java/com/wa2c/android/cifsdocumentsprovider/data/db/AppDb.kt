package com.wa2c.android.cifsdocumentsprovider.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ConnectionSettingEntity::class,
    ],
    version = AppDatabase.DB_VERSION,
    exportSchema = true,
    autoMigrations = [
        AutoMigration (from = 1, to = 2)
    ]
)
internal abstract class AppDatabase : RoomDatabase() {

    abstract fun getStorageSettingDao(): ConnectionSettingDao

    companion object {
        /** DB name */
        private const val DB_NAME = "app.db"
        /** DB version */
        const val DB_VERSION = 2

        /**
         * Build DB
         */
        fun buildDb(context: Context) : AppDatabase {
            return Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
                .build()
        }
    }
}

