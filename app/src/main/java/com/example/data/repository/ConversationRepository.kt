package com.example.data.repository

import com.example.domain.model.Conversation
import com.example.domain.model.ConversationSummary
import com.example.domain.model.Message
import com.example.domain.model.StyleSettings
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
  fun observeConversation(): Flow<Conversation>
  fun observeAllConversationsSummary(): Flow<List<ConversationSummary>>
  fun getCurrentConversation(): Conversation
  suspend fun addMessage(message: Message)
  suspend fun deleteMessage(messageId: String)
  suspend fun clearConversation()
  suspend fun createNewConversation(title: String = "Nouvelle discussion", styleSettings: StyleSettings = StyleSettings()): String
  suspend fun switchConversation(id: String)
  suspend fun renameConversation(id: String, newTitle: String)
  suspend fun deleteConversation(id: String)
  fun searchConversations(query: String): Flow<List<ConversationSummary>>
  suspend fun exportToJson(): String
  suspend fun importFromJson(jsonString: String): Result<Int>
  suspend fun saveCurrentStyleSettings(settings: StyleSettings)
  suspend fun deleteAllData()
}
