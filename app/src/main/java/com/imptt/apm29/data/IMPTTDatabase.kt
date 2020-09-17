package com.imptt.apm29.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.imptt.apm29.config.DATABASE_NAME

/**
 *  author : ciih
 *  date : 2020/9/15 9:08 AM
 *  description :
 */
@Database(entities = [Message::class,User::class,Group::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class IMPTTDatabase : RoomDatabase() {

    abstract fun messageDao():MessageDao
    abstract fun groupDao():GroupDao
    abstract fun userDao():UserDao

    companion object {
        // For Singleton instantiation
        @Volatile private var instance: IMPTTDatabase? = null

        fun getInstance(context: Context): IMPTTDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        // Create and pre-populate the database. See this article for more details:
        // https://medium.com/google-developers/7-pro-tips-for-room-fbadea4bfbd1#4785
        private fun buildDatabase(context: Context): IMPTTDatabase {
            return Room.databaseBuilder(context, IMPTTDatabase::class.java, DATABASE_NAME)
                .build()
        }
    }
}
