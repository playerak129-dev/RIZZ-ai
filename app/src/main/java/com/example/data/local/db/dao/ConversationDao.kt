package com.example.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.db.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

  @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
  fun observeAllConversations(): Flow<List<ConversationEntity>>

  @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
  suspend fun getAllConversations(): List<ConversationEntity>

  @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
  fun observeConversationById(id: String): Flow<ConversationEntity?>

  @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
  suspend fun getConversationById(id: String): ConversationEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertConversation(conversation: ConversationEntity)

  @Update
  suspend fun updateConversation(conversation: ConversationEntity)

  @Query("UPDATE conversations SET title = :newTitle, updatedAt = :updatedAt WHERE id = :id")
  suspend fun updateTitle(id: String, newTitle: String, updatedAt: Long = System.currentTimeMillis())

  @Query("UPDATE conversations SET activeStyle = :style, intensity = :intensity, isBlendModeEnabled = :isBlend, blendWeightsJson = :weightsJson, updatedAt = :updatedAt WHERE id = :id")
  suspend fun updateStyleSettings(
    id: String,
    style: String,
    intensity: Int,
    isBlend: Boolean,
    weightsJson: String,
    updatedAt: Long = System.currentTimeMillis()
  )

  @Query("DELETE FROM conversations WHERE id = :id")
  suspend fun deleteConversationById(id: String)

  @Query("SELECT * FROM conversations WHERE title LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
  fun searchConversationsByTitle(query: String): Flow<List<ConversationEntity>>

  @Query("DELETE FROM conversations")
  suspend fun deleteAllConversations()
}
