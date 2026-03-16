package com.notiledger.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.notiledger.data.model.AppFilter
import com.notiledger.data.model.NotiRecord

@Database(
    entities = [NotiRecord::class, AppFilter::class],
    version = 1,
    exportSchema = false
)
abstract class NotiDatabase : RoomDatabase() {

    abstract fun notiRecordDao(): NotiRecordDao
    abstract fun appFilterDao(): AppFilterDao

    companion object {
        @Volatile
        private var INSTANCE: NotiDatabase? = null

        fun getInstance(context: Context): NotiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NotiDatabase::class.java,
                    "notiledger.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
