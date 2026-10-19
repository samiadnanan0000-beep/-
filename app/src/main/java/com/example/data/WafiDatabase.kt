package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PersonEntity::class, TransactionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class WafiDatabase : RoomDatabase() {

    abstract fun wafiDao(): WafiDao

    companion object {
        @Volatile
        private var INSTANCE: WafiDatabase? = null

        fun getDatabase(context: Context): WafiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WafiDatabase::class.java,
                    "wafi_ledger.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
