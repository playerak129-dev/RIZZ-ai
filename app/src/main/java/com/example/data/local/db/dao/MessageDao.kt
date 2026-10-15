package com.example.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.db.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

  @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
  fun observeMessagesForConversation(conversationId: String): Flow<List<MessageEntity>>

  @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
  suspend fun getMessagesForConversation(conversationId: String): List<MessageEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMessage(message: MessageEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMessages(messages: List<MessageEntity>)

  @Query("DELETE FROM messages WHERE conversationId = :conversationId")
  suspend fun deleteMessagesForConversation(conversationId: String)

  @Query("DELETE FROM messages WHERE id = :messageId")
  suspend fun deleteMessageById(messageId: String)

  @Query("SELECT * FROM messages WHERE content LIKE '%' || :query || '%' ORDER BY timestamp DESC")
  fun searchMessagesByContent(query: String): Flow<List<MessageEntity>>

  @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT 1")
  suspend fun getLastMessageForConversation(conversationId: String): MessageEntity?

  @Query("DELETE FROM messages")
  suspend fun deleteAllMessages()
}
