package com.example.data.repository

import com.example.data.local.db.AppDatabase
import com.example.data.local.db.entity.ConversationEntity
import com.example.data.local.db.entity.MessageEntity
import com.example.domain.model.Conversation
import com.example.domain.model.ConversationStyle
import com.example.domain.model.ConversationSummary
import com.example.domain.model.Message
import com.example.domain.model.MessageRole
import com.example.domain.model.StyleSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class RoomConversationRepository(
  private val database: AppDatabase,
  private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : ConversationRepository {

  private val conversationDao = database.conversationDao()
  private val messageDao = database.messageDao()

  private val activeConversationId = MutableStateFlow<String?>(null)

  init {
    scope.launch {
      val existing = conversationDao.getAllConversations()
      if (existing.isEmpty()) {
        val initialId = UUID.randomUUID().toString()
        val defaultConv = ConversationEntity(
          id = initialId,
          title = "Discussion Principale",
          createdAt = System.currentTimeMillis(),
          updatedAt = System.currentTimeMillis()
        )
        conversationDao.insertConversation(defaultConv)
        activeConversationId.value = initialId
      } else {
        activeConversationId.value = existing.first().id
      }
    }
  }

  @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
  override fun observeConversation(): Flow<Conversation> {
    return activeConversationId.flatMapLatest { convId ->
      if (convId == null) {
        kotlinx.coroutines.flow.flowOf(Conversation())
      } else {
        combine(
          conversationDao.observeConversationById(convId),
          messageDao.observeMessagesForConversation(convId)
        ) { entity, messageEntities ->
          if (entity == null) {
            Conversation(id = convId)
          } else {
            Conversation(
              id = entity.id,
              messages = messageEntities.map { entityToMessage(it) }
            )
          }
        }
      }
    }.distinctUntilChanged().flowOn(Dispatchers.IO)
  }

  override fun observeAllConversationsSummary(): Flow<List<ConversationSummary>> {
    return conversationDao.observeAllConversations().map { convList ->
      convList.map { conv ->
        val lastMsg = messageDao.getLastMessageForConversation(conv.id)
        val style = try {
          ConversationStyle.valueOf(conv.activeStyle)
        } catch (e: Exception) {
          ConversationStyle.NEUTRE
        }
        val allMessages = messageDao.getMessagesForConversation(conv.id)
        ConversationSummary(
          id = conv.id,
          title = conv.title,
          lastMessageSnippet = lastMsg?.content?.take(60) ?: "Aucun message",
          messageCount = allMessages.size,
          activeStyle = style,
          updatedAt = conv.updatedAt
        )
      }
    }.flowOn(Dispatchers.IO)
  }

  override fun getCurrentConversation(): Conversation {
    val convId = activeConversationId.value ?: return Conversation()
    val messages = kotlinx.coroutines.runBlocking(Dispatchers.IO) {
      messageDao.getMessagesForConversation(convId).map { entityToMessage(it) }
    }
    return Conversation(id = convId, messages = messages)
  }

  override suspend fun addMessage(message: Message) = withContext(Dispatchers.IO) {
    var convId = activeConversationId.value
    if (convId == null) {
      convId = createNewConversation("Discussion")
      activeConversationId.value = convId
    }

    val messageEntity = MessageEntity(
      id = message.id,
      conversationId = convId,
      role = message.role.name,
      content = message.content,
      timestamp = message.timestamp,
      imageUri = message.imageUri
    )
    messageDao.insertMessage(messageEntity)

    // Auto update title if this is the first user message
    val conversation = conversationDao.getConversationById(convId)
    if (conversation != null && conversation.title.startsWith("Discussion") && message.role == MessageRole.USER) {
      val autoTitle = message.content.take(28).trim().replace("\n", " ")
      if (autoTitle.isNotBlank()) {
        conversationDao.updateTitle(convId, autoTitle, System.currentTimeMillis())
      } else {
        conversationDao.updateTitle(convId, conversation.title, System.currentTimeMillis())
      }
    } else {
      conversationDao.updateTitle(convId, conversation?.title ?: "Discussion", System.currentTimeMillis())
    }
  }

  override suspend fun clearConversation() = withContext(Dispatchers.IO) {
    val convId = activeConversationId.value ?: return@withContext
    messageDao.deleteMessagesForConversation(convId)
    conversationDao.updateTitle(convId, "Discussion", System.currentTimeMillis())
  }

  override suspend fun createNewConversation(
    title: String,
    styleSettings: StyleSettings
  ): String = withContext(Dispatchers.IO) {
    val newId = UUID.randomUUID().toString()
    val weightsJson = encodeWeightsJson(styleSettings.weights)
    val entity = ConversationEntity(
      id = newId,
      title = title,
      createdAt = System.currentTimeMillis(),
      updatedAt = System.currentTimeMillis(),
      activeStyle = styleSettings.activeStyle.name,
      intensity = styleSettings.intensity,
      isBlendModeEnabled = styleSettings.isBlendModeEnabled,
      blendWeightsJson = weightsJson
    )
    conversationDao.insertConversation(entity)
    activeConversationId.value = newId
    newId
  }

  override suspend fun switchConversation(id: String) = withContext(Dispatchers.IO) {
    val exists = conversationDao.getConversationById(id)
    if (exists != null) {
      activeConversationId.value = id
    }
  }

  override suspend fun renameConversation(id: String, newTitle: String) = withContext(Dispatchers.IO) {
    conversationDao.updateTitle(id, newTitle.trim().take(50), System.currentTimeMillis())
  }

  override suspend fun deleteConversation(id: String) = withContext(Dispatchers.IO) {
    conversationDao.deleteConversationById(id)
    if (activeConversationId.value == id) {
      val remaining = conversationDao.getAllConversations()
      if (remaining.isNotEmpty()) {
        activeConversationId.value = remaining.first().id
      } else {
        val newId = createNewConversation("Discussion Principale")
        activeConversationId.value = newId
      }
    }
  }

  override fun searchConversations(query: String): Flow<List<ConversationSummary>> {
    val trimmed = query.trim()
    return observeAllConversationsSummary().map { list ->
      if (trimmed.isBlank()) list
      else list.filter {
        it.title.contains(trimmed, ignoreCase = true) ||
            it.lastMessageSnippet.contains(trimmed, ignoreCase = true)
      }
    }
  }

  override suspend fun saveCurrentStyleSettings(settings: StyleSettings) = withContext(Dispatchers.IO) {
    val convId = activeConversationId.value ?: return@withContext
    val weightsJson = encodeWeightsJson(settings.weights)
    conversationDao.updateStyleSettings(
      id = convId,
      style = settings.activeStyle.name,
      intensity = settings.intensity,
      isBlend = settings.isBlendModeEnabled,
      weightsJson = weightsJson,
      updatedAt = System.currentTimeMillis()
    )
  }

  override suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
    val root = JSONObject()
    root.put("version", 1)
    root.put("app", "RIZZ AI 2.0 - C4RL3NTZ Edition")
    root.put("exportedAt", System.currentTimeMillis())

    val conversationsJson = JSONArray()
    val allConversations = conversationDao.getAllConversations()

    for (conv in allConversations) {
      val convObj = JSONObject()
      convObj.put("id", conv.id)
      convObj.put("title", conv.title)
      convObj.put("createdAt", conv.createdAt)
      convObj.put("updatedAt", conv.updatedAt)
      convObj.put("activeStyle", conv.activeStyle)
      convObj.put("intensity", conv.intensity)
      convObj.put("isBlendModeEnabled", conv.isBlendModeEnabled)
      convObj.put("blendWeightsJson", conv.blendWeightsJson)

      val messagesJson = JSONArray()
      val messages = messageDao.getMessagesForConversation(conv.id)
      for (msg in messages) {
        val msgObj = JSONObject()
        msgObj.put("id", msg.id)
        msgObj.put("role", msg.role)
        msgObj.put("content", msg.content)
        msgObj.put("timestamp", msg.timestamp)
        if (msg.imageUri != null) {
          msgObj.put("imageUri", msg.imageUri)
        }
        messagesJson.put(msgObj)
      }
      convObj.put("messages", messagesJson)
      conversationsJson.put(convObj)
    }

    root.put("conversations", conversationsJson)
    root.toString(2)
  }

  override suspend fun importFromJson(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
    try {
      val root = JSONObject(jsonString)
      if (!root.has("conversations")) {
        return@withContext Result.failure(IllegalArgumentException("Format JSON invalide : clé 'conversations' manquante."))
      }

      val conversationsArray = root.getJSONArray("conversations")
      var importedCount = 0

      for (i in 0 until conversationsArray.length()) {
        val convObj = conversationsArray.getJSONObject(i)
        val id = convObj.optString("id", UUID.randomUUID().toString())
        val title = convObj.optString("title", "Discussion importée")
        val createdAt = convObj.optLong("createdAt", System.currentTimeMillis())
        val updatedAt = convObj.optLong("updatedAt", System.currentTimeMillis())
        val activeStyle = convObj.optString("activeStyle", "NEUTRE")
        val intensity = convObj.optInt("intensity", 3).coerceIn(1, 5)
        val isBlend = convObj.optBoolean("isBlendModeEnabled", false)
        val weightsJson = convObj.optString("blendWeightsJson", "")

        val convEntity = ConversationEntity(
          id = id,
          title = title,
          createdAt = createdAt,
          updatedAt = updatedAt,
          activeStyle = activeStyle,
          intensity = intensity,
          isBlendModeEnabled = isBlend,
          blendWeightsJson = weightsJson
        )
        conversationDao.insertConversation(convEntity)

        if (convObj.has("messages")) {
          val messagesArray = convObj.getJSONArray("messages")
          val messageEntities = mutableListOf<MessageEntity>()
          for (j in 0 until messagesArray.length()) {
            val msgObj = messagesArray.getJSONObject(j)
            val msgId = msgObj.optString("id", UUID.randomUUID().toString())
            val roleStr = msgObj.optString("role", "USER")
            val content = msgObj.optString("content", "")
            val timestamp = msgObj.optLong("timestamp", System.currentTimeMillis())
            val imageUri = if (msgObj.has("imageUri")) msgObj.getString("imageUri") else null

            messageEntities.add(
              MessageEntity(
                id = msgId,
                conversationId = id,
                role = roleStr,
                content = content,
                timestamp = timestamp,
                imageUri = imageUri
              )
            )
          }
          if (messageEntities.isNotEmpty()) {
            messageDao.insertMessages(messageEntities)
          }
        }
        importedCount++
      }

      if (importedCount > 0 && activeConversationId.value == null) {
        val first = conversationDao.getAllConversations().firstOrNull()
        if (first != null) {
          activeConversationId.value = first.id
        }
      }

      Result.success(importedCount)
    } catch (e: Exception) {
      Result.failure(IllegalArgumentException("Échec du parsing JSON : ${e.localizedMessage ?: "Format incorrect"}"))
    }
  }

  override suspend fun deleteAllData() = withContext(Dispatchers.IO) {
    messageDao.deleteAllMessages()
    conversationDao.deleteAllConversations()
    val newId = createNewConversation("Discussion Principale")
    activeConversationId.value = newId
  }

  private fun entityToMessage(entity: MessageEntity): Message {
    val role = try {
      MessageRole.valueOf(entity.role)
    } catch (e: Exception) {
      MessageRole.USER
    }
    return Message(
      id = entity.id,
      role = role,
      content = entity.content,
      timestamp = entity.timestamp,
      imageUri = entity.imageUri
    )
  }

  private fun encodeWeightsJson(weights: Map<ConversationStyle, Int>): String {
    val obj = JSONObject()
    weights.forEach { (style, weight) ->
      obj.put(style.name, weight)
    }
    return obj.toString()
  }
}
