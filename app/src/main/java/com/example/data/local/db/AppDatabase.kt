package com.example.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.db.dao.ConversationDao
import com.example.data.local.db.dao.MessageDao
import com.example.data.local.db.entity.ConversationEntity
import com.example.data.local.db.entity.MessageEntity

@Database(
  entities = [ConversationEntity::class, MessageEntity::class],
  version = 1,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

  abstract fun conversationDao(): ConversationDao
  abstract fun messageDao(): MessageDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getInstance(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "rizz_ai_database"
        )
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
