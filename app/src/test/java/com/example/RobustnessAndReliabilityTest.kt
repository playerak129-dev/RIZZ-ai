package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.AiConfig
import com.example.data.local.db.AppDatabase
import com.example.data.remote.RemoteAiDataSource
import com.example.data.remote.dto.CandidateDto
import com.example.data.remote.dto.ContentDto
import com.example.data.remote.dto.GeminiApiErrorDto
import com.example.data.remote.dto.GeminiRequestDto
import com.example.data.remote.dto.GeminiResponseDto
import com.example.data.remote.dto.PartDto
import com.example.data.repository.InMemoryConversationRepository
import com.example.data.repository.RoomConversationRepository
import com.example.domain.model.AiError
import com.example.domain.model.AiResult
import com.example.domain.model.Conversation
import com.example.domain.model.ConversationStyle
import com.example.domain.model.Message
import com.example.domain.model.MessageRole
import com.example.domain.model.StyleSettings
import com.example.domain.repository.AiRepository
import com.example.domain.usecase.SendMessageUseCase
import com.example.presentation.components.orb.OrbState
import com.example.presentation.conversation.ConversationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class RobustnessAndReliabilityTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var inMemoryDb: AppDatabase

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    val context = ApplicationProvider.getApplicationContext<Context>()
    inMemoryDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
  }

  @After
  fun tearDown() {
    inMemoryDb.close()
    Dispatchers.resetMain()
  }

  // --- 1 & 2. Envoi de message, réception et affichage ---

  @Test
  fun `1 and 2 - sending message appends user turn then assistant turn cleanly`() = runTest {
    val fakeRepo = AiIntegrationUnitTest.FakeAiRepository().apply {
      resultToReturn = AiResult.Success("Voici une répartie pleine d'esprit 😉")
    }
    val useCase = SendMessageUseCase(fakeRepo)
    val conversationRepo = InMemoryConversationRepository()
    val viewModel = ConversationViewModel(conversationRepo, useCase)
    advanceUntilIdle()

    viewModel.onInputTextChanged("Salut, comment briser la glace ?")
    viewModel.sendMessage()
    advanceUntilIdle()

    val messages = conversationRepo.getCurrentConversation().messages
    assertEquals(2, messages.size)
    assertEquals("Salut, comment briser la glace ?", messages[0].content)
    assertEquals(MessageRole.USER, messages[0].role)
    assertEquals("Voici une répartie pleine d'esprit 😉", messages[1].content)
    assertEquals(MessageRole.ASSISTANT, messages[1].role)

    assertFalse(viewModel.uiState.value.isSending)
    assertNull(viewModel.uiState.value.errorMessage)
  }

  // --- 3. Conservation du contexte conversationnel ---

  @Test
  fun `3 - conversation history retains previous turns up to max context`() = runTest {
    val conversationRepo = InMemoryConversationRepository()
    for (i in 1..6) {
      conversationRepo.addMessage(Message(role = MessageRole.USER, content = "Question $i"))
      conversationRepo.addMessage(Message(role = MessageRole.ASSISTANT, content = "Réponse $i"))
    }

    val conversation = conversationRepo.getCurrentConversation()
    assertEquals(12, conversation.messages.size)

    val validContext = conversation.messages
      .filter { it.role == MessageRole.USER || it.role == MessageRole.ASSISTANT }
      .takeLast(AiConfig.MAX_CONTEXT_MESSAGES)

    assertEquals(10, validContext.size)
    assertEquals("Question 2", validContext.first().content)
    assertEquals("Réponse 6", validContext.last().content)
  }

  // --- 4. Maintien du style sélectionné ---

  @Test
  fun `4 - selected style and intensity are preserved across turns`() = runTest {
    val fakeRepo = AiIntegrationUnitTest.FakeAiRepository()
    val useCase = SendMessageUseCase(fakeRepo)
    val conversationRepo = InMemoryConversationRepository()
    val viewModel = ConversationViewModel(conversationRepo, useCase)
    advanceUntilIdle()

    viewModel.selectStyle(ConversationStyle.ROMANTIQUE)
    viewModel.setStyleIntensity(5)

    viewModel.onInputTextChanged("Premier tour")
    viewModel.sendMessage()
    advanceUntilIdle()

    assertEquals(ConversationStyle.ROMANTIQUE, fakeRepo.lastReceivedStyleSettings?.activeStyle)
    assertEquals(5, fakeRepo.lastReceivedStyleSettings?.intensity)

    // Second turn maintains the exact same style
    viewModel.onInputTextChanged("Second tour")
    viewModel.sendMessage()
    advanceUntilIdle()

    assertEquals(ConversationStyle.ROMANTIQUE, fakeRepo.lastReceivedStyleSettings?.activeStyle)
    assertEquals(5, fakeRepo.lastReceivedStyleSettings?.intensity)
  }

  // --- 5. Gestion des réponses vides, nulles ou bloquées par sécurité ---

  @Test
  fun `5 - empty response or safety finishReason is handled with specific error`() = runTest {
    var callAttempts = 0
    val mockService = object : com.example.data.remote.GeminiApiService {
      override suspend fun generateContent(
        model: String,
        apiKey: String,
        request: GeminiRequestDto
      ): GeminiResponseDto {
        callAttempts++
        return GeminiResponseDto(
          candidates = listOf(
            CandidateDto(
              content = null,
              finishReason = "SAFETY"
            )
          )
        )
      }
    }

    val dataSource = RemoteAiDataSource(mockService)
    val result = dataSource.generateContent("dummy_key", GeminiRequestDto(emptyList()))

    assertTrue(result is AiResult.Error)
    val error = (result as AiResult.Error).error
    assertTrue(error.message.contains("sécurité"))
    // Safety blocks are NOT retried automatically
    assertEquals(1, callAttempts)
  }

  // --- 6. Erreurs réseau et API avec nouvelles tentatives limitées ---

  @Test
  fun `6 - transient network timeout triggers limited retries with backoff`() = runTest {
    var attempts = 0
    val mockService = object : com.example.data.remote.GeminiApiService {
      override suspend fun generateContent(
        model: String,
        apiKey: String,
        request: GeminiRequestDto
      ): GeminiResponseDto {
        attempts++
        if (attempts < 3) {
          throw SocketTimeoutException("Simulated timeout")
        }
        return GeminiResponseDto(
          candidates = listOf(
            CandidateDto(
              content = ContentDto(
                parts = listOf(PartDto(text = "Succès après reprise réseau"))
              )
            )
          )
        )
      }
    }

    val dataSource = RemoteAiDataSource(mockService)
    val result = dataSource.generateContent("dummy_key", GeminiRequestDto(emptyList()))

    assertTrue(result is AiResult.Success)
    assertEquals("Succès après reprise réseau", (result as AiResult.Success).data)
    assertEquals(3, attempts) // 1 initial + 2 retries
  }

  @Test
  fun `6 - quota exceeded 429 does not perform unnecessary retries`() = runTest {
    var attempts = 0
    val mockService = object : com.example.data.remote.GeminiApiService {
      override suspend fun generateContent(
        model: String,
        apiKey: String,
        request: GeminiRequestDto
      ): GeminiResponseDto {
        attempts++
        val responseBody = "Quota exceeded".toResponseBody("text/plain".toMediaTypeOrNull())
        throw HttpException(Response.error<GeminiResponseDto>(429, responseBody))
      }
    }

    val dataSource = RemoteAiDataSource(mockService)
    val result = dataSource.generateContent("dummy_key", GeminiRequestDto(emptyList()))

    assertTrue(result is AiResult.Error)
    assertEquals(AiError.QuotaExceeded, (result as AiResult.Error).error)
    assertEquals(1, attempts) // Permanent error is not retried
  }

  // --- 7. Nouvelles tentatives sans doublons ---

  @Test
  fun `7 - retry last message does not create duplicate user turn`() = runTest {
    val fakeRepo = AiIntegrationUnitTest.FakeAiRepository().apply {
      resultToReturn = AiResult.Error(AiError.NetworkError)
    }
    val useCase = SendMessageUseCase(fakeRepo)
    val conversationRepo = InMemoryConversationRepository()
    val viewModel = ConversationViewModel(conversationRepo, useCase)
    advanceUntilIdle()

    viewModel.onInputTextChanged("Message original")
    viewModel.sendMessage()
    advanceUntilIdle()

    // 1 user message stored, 0 assistant messages
    assertEquals(1, conversationRepo.getCurrentConversation().messages.size)
    assertTrue(viewModel.uiState.value.canRetry)

    // Now resolve error and retry
    fakeRepo.resultToReturn = AiResult.Success("Réponse valide")
    viewModel.retryLastMessage()
    advanceUntilIdle()

    // Still exactly 1 user message + 1 assistant message (NO duplicate user turn!)
    val messages = conversationRepo.getCurrentConversation().messages
    assertEquals(2, messages.size)
    assertEquals("Message original", messages[0].content)
    assertEquals(MessageRole.USER, messages[0].role)
    assertEquals("Réponse valide", messages[1].content)
    assertEquals(MessageRole.ASSISTANT, messages[1].role)
  }

  @Test
  fun `7 - regenerate assistant message deletes previous assistant response without duplication`() = runTest {
    val fakeRepo = AiIntegrationUnitTest.FakeAiRepository().apply {
      resultToReturn = AiResult.Success("Première tentative assistant")
    }
    val useCase = SendMessageUseCase(fakeRepo)
    val conversationRepo = InMemoryConversationRepository()
    val viewModel = ConversationViewModel(conversationRepo, useCase)
    advanceUntilIdle()

    viewModel.onInputTextChanged("Donne-moi une idée")
    viewModel.sendMessage()
    advanceUntilIdle()

    val messages = conversationRepo.getCurrentConversation().messages
    assertEquals(2, messages.size)
    val oldAssistantId = messages[1].id

    // Regenerate
    fakeRepo.resultToReturn = AiResult.Success("Nouvelle idée plus audacieuse")
    viewModel.regenerateAssistantMessage(oldAssistantId)
    advanceUntilIdle()

    val updatedMessages = conversationRepo.getCurrentConversation().messages
    assertEquals(2, updatedMessages.size)
    assertEquals("Nouvelle idée plus audacieuse", updatedMessages[1].content)
  }

  // --- 8. Persistance et reprise d'une conversation dans Room ---

  @Test
  fun `8 - room persists conversation order and restores most recent on restart`() = runTest {
    val repo = RoomConversationRepository(inMemoryDb)
    advanceUntilIdle()

    val convId = repo.createNewConversation("Session Tinder")
    repo.switchConversation(convId)
    repo.addMessage(Message(role = MessageRole.USER, content = "Premier échange"))
    repo.addMessage(Message(role = MessageRole.ASSISTANT, content = "Superbe amorce"))

    // Simulate app restart by creating a new repository with the same underlying database
    val restartedRepo = RoomConversationRepository(inMemoryDb)
    advanceUntilIdle()

    val restoredConv = restartedRepo.observeConversation().first()
    assertEquals(convId, restoredConv.id)
    assertEquals(2, restoredConv.messages.size)
    assertEquals("Premier échange", restoredConv.messages[0].content)
    assertEquals("Superbe amorce", restoredConv.messages[1].content)
  }

  // --- 9. Prévention des doubles envois ---

  @Test
  fun `10 - double click on send while in flight is strictly prevented`() = runTest {
    val fakeRepo = AiIntegrationUnitTest.FakeAiRepository().apply {
      delayMs = 2000L
    }
    val useCase = SendMessageUseCase(fakeRepo)
    val conversationRepo = InMemoryConversationRepository()
    val viewModel = ConversationViewModel(conversationRepo, useCase)
    advanceUntilIdle()

    viewModel.onInputTextChanged("Test envoi unique")
    viewModel.sendMessage()
    testScheduler.runCurrent()

    assertTrue(viewModel.uiState.value.isSending)

    // Accidental second click immediately
    viewModel.sendMessage()
    testScheduler.runCurrent()

    assertEquals(1, fakeRepo.callCount)
    advanceUntilIdle()
  }

  // --- 11 & 12. Conservation d'état pendant rotation et recréation d'activité ---

  @Test
  fun `11 and 12 - uiState text and settings remain stable without extra Gemini calls`() = runTest {
    val fakeRepo = AiIntegrationUnitTest.FakeAiRepository()
    val useCase = SendMessageUseCase(fakeRepo)
    val conversationRepo = InMemoryConversationRepository()
    val viewModel = ConversationViewModel(conversationRepo, useCase)
    advanceUntilIdle()

    viewModel.onInputTextChanged("Brouillon de message non encore envoyé")
    viewModel.selectStyle(ConversationStyle.CHARO)
    viewModel.setStyleIntensity(4)

    // Activity rotation retains the ViewModel instance
    val state = viewModel.uiState.value
    assertEquals("Brouillon de message non encore envoyé", state.inputText)
    assertEquals(ConversationStyle.CHARO, state.styleSettings.activeStyle)
    assertEquals(4, state.styleSettings.intensity)
    // No spurious Gemini API call was executed
    assertEquals(0, fakeRepo.callCount)
  }

  // --- 13. Précision du diagnostic d'erreur HTTP et fallback de modèle ---

  @Test
  fun `13 - http 401 returns ApiKeyInvalid distinguishing it from missing key`() = runTest {
    val mockService = object : com.example.data.remote.GeminiApiService {
      override suspend fun generateContent(
        model: String,
        apiKey: String,
        request: GeminiRequestDto
      ): GeminiResponseDto {
        val body = "{\"error\":{\"message\":\"API key not valid\"}}".toResponseBody("application/json".toMediaTypeOrNull())
        throw HttpException(Response.error<GeminiResponseDto>(401, body))
      }
    }

    val dataSource = RemoteAiDataSource(mockService)
    val result = dataSource.generateContent("invalid_key", GeminiRequestDto(emptyList()))

    assertTrue(result is AiResult.Error)
    assertEquals(AiError.ApiKeyInvalid, (result as AiResult.Error).error)
  }

  @Test
  fun `13 - http 403 returns PermissionDenied`() = runTest {
    val mockService = object : com.example.data.remote.GeminiApiService {
      override suspend fun generateContent(
        model: String,
        apiKey: String,
        request: GeminiRequestDto
      ): GeminiResponseDto {
        val body = "{\"error\":{\"message\":\"Permission denied\"}}".toResponseBody("application/json".toMediaTypeOrNull())
        throw HttpException(Response.error<GeminiResponseDto>(403, body))
      }
    }

    val dataSource = RemoteAiDataSource(mockService)
    val result = dataSource.generateContent("restricted_key", GeminiRequestDto(emptyList()))

    assertTrue(result is AiResult.Error)
    assertEquals(AiError.PermissionDenied, (result as AiResult.Error).error)
  }

  @Test
  fun `13 - http 400 returns BadRequest`() = runTest {
    val mockService = object : com.example.data.remote.GeminiApiService {
      override suspend fun generateContent(
        model: String,
        apiKey: String,
        request: GeminiRequestDto
      ): GeminiResponseDto {
        val body = "{\"error\":{\"message\":\"Bad Request\"}}".toResponseBody("application/json".toMediaTypeOrNull())
        throw HttpException(Response.error<GeminiResponseDto>(400, body))
      }
    }

    val dataSource = RemoteAiDataSource(mockService)
    val result = dataSource.generateContent("valid_key", GeminiRequestDto(emptyList()))

    assertTrue(result is AiResult.Error)
    assertEquals(AiError.BadRequest, (result as AiResult.Error).error)
  }

  @Test
  fun `13 - http 404 triggers model fallback and succeeds if fallback exists`() = runTest {
    val modelsRequested = mutableListOf<String>()
    val mockService = object : com.example.data.remote.GeminiApiService {
      override suspend fun generateContent(
        model: String,
        apiKey: String,
        request: GeminiRequestDto
      ): GeminiResponseDto {
        modelsRequested.add(model)
        if (model == AiConfig.MODEL_NAME) {
          val body = "{\"error\":{\"message\":\"Model not found\"}}".toResponseBody("application/json".toMediaTypeOrNull())
          throw HttpException(Response.error<GeminiResponseDto>(404, body))
        }
        return GeminiResponseDto(
          candidates = listOf(
            CandidateDto(
              content = ContentDto(
                parts = listOf(PartDto(text = "Succès avec modèle de repli"))
              )
            )
          )
        )
      }
    }

    val dataSource = RemoteAiDataSource(mockService)
    val result = dataSource.generateContent("valid_key", GeminiRequestDto(emptyList()))

    assertTrue(result is AiResult.Success)
    assertEquals("Succès avec modèle de repli", (result as AiResult.Success).data)
    assertEquals(listOf(AiConfig.MODEL_NAME, AiConfig.FALLBACK_MODEL_NAME), modelsRequested)
  }

  @Test
  fun `13 - http 503 returns ServerUnavailable after controlled retries`() = runTest {
    var attempts = 0
    val mockService = object : com.example.data.remote.GeminiApiService {
      override suspend fun generateContent(
        model: String,
        apiKey: String,
        request: GeminiRequestDto
      ): GeminiResponseDto {
        attempts++
        val body = "{\"error\":{\"message\":\"Service Unavailable\"}}".toResponseBody("application/json".toMediaTypeOrNull())
        throw HttpException(Response.error<GeminiResponseDto>(503, body))
      }
    }

    val dataSource = RemoteAiDataSource(mockService)
    val result = dataSource.generateContent("valid_key", GeminiRequestDto(emptyList()))

    assertTrue(result is AiResult.Error)
    assertEquals(AiError.ServerUnavailable, (result as AiResult.Error).error)
    assertEquals(3, attempts) // 1 initial + 2 retries
  }
}
