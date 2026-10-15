package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.AiConfig
import com.example.core.SecurityUtils
import com.example.data.local.db.AppDatabase
import com.example.data.local.db.entity.ConversationEntity
import com.example.data.local.db.entity.MessageEntity
import com.example.data.repository.InMemoryConversationRepository
import com.example.data.repository.RoomConversationRepository
import com.example.domain.model.ConversationStyle
import com.example.domain.model.Message
import com.example.domain.model.MessageRole
import com.example.domain.model.StyleSettings
import com.example.domain.model.strategic.StrategicBoardResult
import com.example.domain.model.strategic.StrategicBranch
import com.example.domain.model.strategic.StrategicOption
import com.example.presentation.conversation.ConversationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class Phase5And6FullTestSuite {

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

  // --- 1. Styles, Intensité et Normalisation à 100% ---

  @Test
  fun `style settings defaults and intensity bounds check`() {
    val defaultSettings = StyleSettings()
    assertEquals(ConversationStyle.NEUTRE, defaultSettings.activeStyle)
    assertEquals(3, defaultSettings.intensity)
    assertFalse(defaultSettings.isBlendModeEnabled)

    val validMin = StyleSettings(intensity = 1)
    val validMax = StyleSettings(intensity = 5)
    assertEquals(1, validMin.intensity)
    assertEquals(5, validMax.intensity)
  }

  @Test
  fun `style blend mode guarantees mathematical sum of exactly 100 percent`() {
    val settings = StyleSettings(
      isBlendModeEnabled = true,
      weights = mapOf(
        ConversationStyle.CHARO to 50,
        ConversationStyle.HUMOUR to 30,
        ConversationStyle.ROMANTIQUE to 20,
        ConversationStyle.CALCULATEUR to 0
      )
    )
    val normalized = settings.getNormalizedWeights()
    val total = normalized.values.sum()
    assertEquals(100, total)
  }

  @Test
  fun `weight adjustment keeps total exactly 100 percent`() {
    val initial = StyleSettings()
    val adjusted = initial.withAdjustedWeight(ConversationStyle.CHARO, 60)
    val total = adjusted.weights.values.sum()
    assertEquals(100, total)
    assertEquals(60, adjusted.weights[ConversationStyle.CHARO])
  }

  @Test
  fun `all conversation styles have non-empty metadata`() {
    for (style in ConversationStyle.values()) {
      assertTrue(style.displayName.isNotBlank())
      assertTrue(style.emoji.isNotBlank())
      assertTrue(style.tagLine.isNotBlank())
    }
  }

  // --- 2. Sécurité et Code PIN Local ---

  @Test
  fun `security utils hashes PIN and verifies correctly`() {
    val rawPin = "1234"
    val hash = SecurityUtils.hashPin(rawPin)

    assertNotNull(hash)
    assertTrue(hash.length >= 32)
    assertNotEquals("1234", hash)

    assertTrue(SecurityUtils.verifyPin("1234", hash))
    assertFalse(SecurityUtils.verifyPin("0000", hash))
    assertFalse(SecurityUtils.verifyPin("1235", hash))
    assertFalse(SecurityUtils.verifyPin("", hash))
  }

  // --- 3. Base de données Room & Persistance locale ---

  @Test
  fun `room inserts conversation and messages and cascades on delete`() = runTest {
    val convDao = inMemoryDb.conversationDao()
    val msgDao = inMemoryDb.messageDao()

    val convId = UUID.randomUUID().toString()
    val conv = ConversationEntity(
      id = convId,
      title = "Test Seduction",
      createdAt = 1000L,
      updatedAt = 2000L
    )
    convDao.insertConversation(conv)

    val retrievedConv = convDao.getConversationById(convId)
    assertNotNull(retrievedConv)
    assertEquals("Test Seduction", retrievedConv?.title)

    val msgId1 = UUID.randomUUID().toString()
    val msgId2 = UUID.randomUUID().toString()
    msgDao.insertMessage(MessageEntity(id = msgId1, conversationId = convId, role = "USER", content = "Coucou"))
    msgDao.insertMessage(MessageEntity(id = msgId2, conversationId = convId, role = "ASSISTANT", content = "Salut beauté"))

    val messages = msgDao.getMessagesForConversation(convId)
    assertEquals(2, messages.size)

    // Deleting conversation cascades to messages
    convDao.deleteConversationById(convId)
    val remainingMessages = msgDao.getMessagesForConversation(convId)
    assertEquals(0, remainingMessages.size)
  }

  @Test
  fun `room conversation repository search and JSON export import`() = runTest {
    val repo = RoomConversationRepository(inMemoryDb)
    advanceUntilIdle()

    val convId = repo.createNewConversation("Rencontre Tinder")
    repo.switchConversation(convId)
    repo.addMessage(Message(role = MessageRole.USER, content = "Dis-moi une phrase mystérieuse"))
    repo.addMessage(Message(role = MessageRole.ASSISTANT, content = "Le hasard ne fait jamais d'erreurs."))

    val exportedJson = repo.exportToJson()
    assertTrue(exportedJson.contains("Rencontre Tinder"))
    assertTrue(exportedJson.contains("Le hasard ne fait jamais d'erreurs."))

    // Clear data then import back
    repo.deleteAllData()
    val importResult = repo.importFromJson(exportedJson)
    assertTrue(importResult.isSuccess)
    assertTrue((importResult.getOrNull() ?: 0) >= 1)

    val allSummaries = repo.observeAllConversationsSummary().first()
    assertTrue(allSummaries.any { it.title == "Rencontre Tinder" })
  }

  @Test
  fun `importing invalid JSON returns failure without crashing`() = runTest {
    val repo = RoomConversationRepository(inMemoryDb)
    val invalidJson = "{\"invalid\": 123}"
    val result = repo.importFromJson(invalidJson)
    assertTrue(result.isFailure)
  }

  // --- 4. Échiquier Stratégique & Hypothèses d'anticipation ---

  @Test
  fun `strategic board result contains mandatory disclaimer`() {
    val board = StrategicBoardResult(
      situation = "Message reçu",
      analysis = "Tension positive",
      coachAdvice = "Rester calme",
      options = listOf(
        StrategicOption(
          id = "1",
          name = "Audace",
          proposedReply = "J'arrive",
          tone = "Charo",
          score = 90,
          isRecommended = true,
          strengths = listOf("Rapide"),
          risks = listOf("Direct"),
          branches = listOf(
            StrategicBranch(
              trigger = "Si enthousiaste",
              hypothesis = "Ouverture",
              counterReply = "Parfait"
            )
          )
        )
      )
    )

    assertTrue(board.disclaimer.contains("hypothèses", ignoreCase = true))
    assertEquals(1, board.options.size)
    assertEquals("Audace", board.options.first().name)
  }

  // --- 5. ViewModel Actions & State Controls ---

  @Test
  fun `viewModel history dialog and renaming controls`() = runTest {
    val repo = InMemoryConversationRepository()
    val viewModel = ConversationViewModel(repository = repo)
    advanceUntilIdle()

    viewModel.openHistory()
    assertTrue(viewModel.uiState.value.isHistoryOpen)

    viewModel.updateHistorySearchQuery("Tinder")
    assertEquals("Tinder", viewModel.uiState.value.historySearchQuery)

    viewModel.closeHistory()
    assertFalse(viewModel.uiState.value.isHistoryOpen)

    viewModel.openRenameDialog("id_123", "Ancien Titre")
    assertTrue(viewModel.uiState.value.isRenameDialogOpen)
    assertEquals("Ancien Titre", viewModel.uiState.value.renamingConversationTitle)

    viewModel.updateRenamingTitle("Nouveau Titre")
    assertEquals("Nouveau Titre", viewModel.uiState.value.renamingConversationTitle)

    viewModel.dismissRenameDialog()
    assertFalse(viewModel.uiState.value.isRenameDialogOpen)
  }

  @Test
  fun `viewModel strategic board and coach mode controls`() = runTest {
    val repo = InMemoryConversationRepository()
    val viewModel = ConversationViewModel(repository = repo)

    viewModel.openStrategicBoard()
    assertTrue(viewModel.uiState.value.isStrategicBoardOpen)

    viewModel.updateStrategicSituation("Elle a répondu avec un émoji rire")
    assertEquals("Elle a répondu avec un émoji rire", viewModel.uiState.value.strategicSituation)

    viewModel.toggleCoachMode(true)
    assertTrue(viewModel.uiState.value.isCoachModeActive)

    viewModel.closeStrategicBoard()
    assertFalse(viewModel.uiState.value.isStrategicBoardOpen)
  }

  @Test
  fun `viewModel assistant message reuse injects text into input`() = runTest {
    val repo = InMemoryConversationRepository()
    val viewModel = ConversationViewModel(repository = repo)

    viewModel.reuseAssistantMessage("Une superbe punchline")
    assertEquals("Une superbe punchline", viewModel.uiState.value.inputText)
    assertEquals(com.example.presentation.components.orb.OrbState.FOCUSED, viewModel.uiState.value.orbState)
  }
}
