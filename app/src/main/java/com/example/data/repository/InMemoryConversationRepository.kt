package com.example.data.repository

import com.example.domain.model.Conversation
import com.example.domain.model.ConversationStyle
import com.example.domain.model.ConversationSummary
import com.example.domain.model.Message
import com.example.domain.model.StyleSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class InMemoryConversationRepository : ConversationRepository {
  private val _conversation = MutableStateFlow(Conversation(id = "default"))
  private val _conversationsMap = MutableStateFlow<Map<String, Conversation>>(mapOf("default" to Conversation(id = "default")))
  private val _titles = MutableStateFlow<Map<String, String>>(mapOf("default" to "Discussion Principale"))

  override fun observeConversation(): Flow<Conversation> = _conversation.asStateFlow()

  override fun observeAllConversationsSummary(): Flow<List<ConversationSummary>> {
    return _conversationsMap.map { map ->
      map.values.map { conv ->
        ConversationSummary(
          id = conv.id,
          title = _titles.value[conv.id] ?: "Discussion",
          lastMessageSnippet = conv.messages.lastOrNull()?.content?.take(50) ?: "Aucun message",
          messageCount = conv.messages.size,
          activeStyle = ConversationStyle.NEUTRE,
          updatedAt = System.currentTimeMillis()
        )
      }
    }
  }

  override fun getCurrentConversation(): Conversation = _conversation.value

  override suspend fun addMessage(message: Message) {
    _conversation.update { current ->
      current.copy(messages = current.messages + message)
    }
    _conversationsMap.update { map ->
      map + (_conversation.value.id to _conversation.value)
    }
  }

  override suspend fun deleteMessage(messageId: String) {
    _conversation.update { current ->
      current.copy(messages = current.messages.filter { it.id != messageId })
    }
    _conversationsMap.update { map ->
      map + (_conversation.value.id to _conversation.value)
    }
  }

  override suspend fun clearConversation() {
    _conversation.update { current ->
      current.copy(messages = emptyList())
    }
    _conversationsMap.update { map ->
      map + (_conversation.value.id to _conversation.value)
    }
  }

  override suspend fun createNewConversation(title: String, styleSettings: StyleSettings): String {
    val newId = UUID.randomUUID().toString()
    val newConv = Conversation(id = newId)
    _titles.update { it + (newId to title) }
    _conversationsMap.update { it + (newId to newConv) }
    _conversation.value = newConv
    return newId
  }

  override suspend fun switchConversation(id: String) {
    val found = _conversationsMap.value[id]
    if (found != null) {
      _conversation.value = found
    }
  }

  override suspend fun renameConversation(id: String, newTitle: String) {
    _titles.update { it + (id to newTitle) }
  }

  override suspend fun deleteConversation(id: String) {
    _conversationsMap.update { it - id }
    _titles.update { it - id }
    if (_conversation.value.id == id) {
      val next = _conversationsMap.value.values.firstOrNull() ?: Conversation(id = "default")
      _conversation.value = next
    }
  }

  override fun searchConversations(query: String): Flow<List<ConversationSummary>> {
    return observeAllConversationsSummary().map { list ->
      if (query.isBlank()) list
      else list.filter { it.title.contains(query, ignoreCase = true) }
    }
  }

  override suspend fun exportToJson(): String {
    val root = JSONObject()
    root.put("version", 1)
    root.put("app", "RIZZ AI 2.0 - C4RL3NTZ Edition")
    root.put("conversations", JSONArray())
    return root.toString()
  }

  override suspend fun importFromJson(jsonString: String): Result<Int> {
    return Result.success(0)
  }

  override suspend fun saveCurrentStyleSettings(settings: StyleSettings) {}

  override suspend fun deleteAllData() {
    clearConversation()
  }
}
