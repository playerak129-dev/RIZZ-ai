package com.example

import com.example.core.AiConfig
import com.example.data.repository.GeminiAiRepository
import com.example.data.repository.InMemoryConversationRepository
import com.example.domain.model.AiError
import com.example.domain.model.AiResult
import com.example.domain.model.AttachedImage
import com.example.domain.model.Conversation
import com.example.domain.model.ConversationStyle
import com.example.domain.model.Message
import com.example.domain.model.MessageRole
import com.example.domain.model.StyleSettings
import com.example.domain.repository.AiRepository
import com.example.domain.usecase.RizzRapideUseCase
import com.example.domain.usecase.SendMessageUseCase
import com.example.presentation.components.orb.OrbState
import com.example.presentation.conversation.ConversationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiIntegrationUnitTest {

  private val testDispatcher = StandardTestDispatcher()

  class FakeAiRepository : AiRepository {
    var resultToReturn: AiResult<String> = AiResult.Success("Excellente amorce !")
    var rizzRapideResult: AiResult<List<String>> = AiResult.Success(
      listOf("Option 1 : Audacieuse", "Option 2 : Taquine", "Option 3 : Intrigante")
    )
    var delayMs: Long = 0L
    var lastReceivedConversation: Conversation? = null
    var lastReceivedPrompt: String? = null
    var lastReceivedImage: AttachedImage? = null
    var lastReceivedStyleSettings: StyleSettings? = null
    var lastReceivedSituation: String? = null
    var callCount: Int = 0
    var rizzRapideCallCount: Int = 0

    override suspend fun generateResponse(
      conversation: Conversation,
      userPrompt: String,
      attachedImage: AttachedImage?,
      styleSettings: StyleSettings
    ): AiResult<String> {
      callCount++
      lastReceivedConversation = conversation
      lastReceivedPrompt = userPrompt
      lastReceivedImage = attachedImage
      lastReceivedStyleSettings = styleSettings
      if (delayMs > 0) delay(delayMs)
      return resultToReturn
    }

    override suspend fun generateRizzRapide(
      situation: String,
      styleSettings: StyleSettings
    ): AiResult<List<String>> {
      rizzRapideCallCount++
      lastReceivedSituation = situation
      lastReceivedStyleSettings = styleSettings
      if (delayMs > 0) delay(delayMs)
      return rizzRapideResult
    }

    override suspend fun generateStrategicBoard(
      situation: String,
      styleSettings: StyleSettings
    ): AiResult<com.example.domain.model.strategic.StrategicBoardResult> {
      return AiResult.Success(
        com.example.domain.model.strategic.StrategicBoardResult(
          situation = situation,
          analysis = "Analyse test",
          coachAdvice = "Conseil coach test",
          options = listOf(
            com.example.domain.model.strategic.StrategicOption(
              id = "opt_test",
              name = "Option Test",
              proposedReply = "Salut toi",
              tone = "Audacieux",
              score = 90,
              isRecommended = true,
              strengths = listOf("Direct"),
              risks = listOf("Tranché"),
              branches = listOf(
                com.example.domain.model.strategic.StrategicBranch(
                  trigger = "Si enthousiasme",
                  hypothesis = "Intérêt",
                  counterReply = "On se voit bientôt"
                )
              )
            )
          )
        )
      )
    }

    override suspend fun analyzeRealReply(
      situation: String,
      chosenOption: String,
      realReceivedReply: String
    ): AiResult<com.example.domain.model.strategic.ComparisonResult> {
      return AiResult.Success(
        com.example.domain.model.strategic.ComparisonResult(
          receivedReply = realReceivedReply,
          matchedBranch = "Branche A",
          interpretation = "Réaction positive",
          recommendedNextMove = "Relance fluide",
          coachInsight = "Garder le cap"
        )
      )
    }
  }

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `sendMessageUseCase rejects empty prompt without image`() = runTest {
    val fakeRepo = FakeAiRepository()
    val useCase = SendMessageUseCase(fakeRepo)

    val result = useCase(Conversation(), "   ", null)
    assertTrue(result is AiResult.Error)
    assertEquals(0, fakeRepo.callCount)
  }

  @Test
  fun `sendMessageUseCase accepts empty text when image is attached`() = runTest {
    val fakeRepo = FakeAiRepository()
    val useCase = SendMessageUseCase(fakeRepo)
    val image = AttachedImage(uri = "content://media/1", base64Data = "abc", mimeType = "image/jpeg")

    val result = useCase(Conversation(), "", image)
    assertTrue(result is AiResult.Success)
    assertEquals(1, fakeRepo.callCount)
    assertEquals(image, fakeRepo.lastReceivedImage)
  }

  @Test
  fun `viewModel successful AI response updates conversation and cycles orb state`() = runTest {
    val fakeRepo = FakeAiRepository().apply {
      resultToReturn = AiResult.Success("Voici une punchline percutante")
      delayMs = 1000
    }
    val useCase = SendMessageUseCase(fakeRepo)
    val conversationRepo = InMemoryConversationRepository()
    val viewModel = ConversationViewModel(conversationRepo, useCase)
    advanceUntilIdle()

    viewModel.onInputTextChanged("Donne-moi une amorce")
    viewModel.sendMessage()
    testScheduler.runCurrent()

    // While request is in-flight
    assertTrue(viewModel.uiState.value.isSending)
    assertEquals(OrbState.THINKING, viewModel.uiState.value.orbState)

    advanceUntilIdle()

    // Request finished
    assertFalse(viewModel.uiState.value.isSending)
    assertEquals(2, conversationRepo.getCurrentConversation().messages.size)

    val userMsg = conversationRepo.getCurrentConversation().messages[0]
    val aiMsg = conversationRepo.getCurrentConversation().messages[1]

    assertEquals("Donne-moi une amorce", userMsg.content)
    assertEquals(MessageRole.USER, userMsg.role)

    assertEquals("Voici une punchline percutante", aiMsg.content)
    assertEquals(MessageRole.ASSISTANT, aiMsg.role)

    // Check transition back to IDLE after delay
    advanceTimeBy(1500)
    assertEquals(OrbState.IDLE, viewModel.uiState.value.orbState)
  }

  @Test
  fun `viewModel network error sets error message and enables retry`() = runTest {
    val fakeRepo = FakeAiRepository().apply {
      resultToReturn = AiResult.Error(AiError.NetworkError)
    }
    val useCase = SendMessageUseCase(fakeRepo)
    val conversationRepo = InMemoryConversationRepository()
    val viewModel = ConversationViewModel(conversationRepo, useCase)
    advanceUntilIdle()

    viewModel.onInputTextChanged("Message qui échoue")
    viewModel.sendMessage()
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isSending)
    assertEquals(AiError.NetworkError.message, state.errorMessage)
    assertTrue(state.canRetry)

    // Now test retry
    fakeRepo.resultToReturn = AiResult.Success("Réponse après retry")
    viewModel.retryLastMessage()
    advanceUntilIdle()

    val retriedState = viewModel.uiState.value
    assertNull(retriedState.errorMessage)
    assertFalse(retriedState.canRetry)
    assertEquals(2, conversationRepo.getCurrentConversation().messages.size)
    assertEquals("Réponse après retry", conversationRepo.getCurrentConversation().messages[1].content)
  }

  @Test
  fun `viewModel prevents double submission while in flight`() = runTest {
    val fakeRepo = FakeAiRepository().apply {
      delayMs = 2000
    }
    val useCase = SendMessageUseCase(fakeRepo)
    val conversationRepo = InMemoryConversationRepository()
    val viewModel = ConversationViewModel(conversationRepo, useCase)
    advanceUntilIdle()

    viewModel.onInputTextChanged("Premier envoi")
    viewModel.sendMessage()
    testScheduler.runCurrent()

    assertTrue(viewModel.uiState.value.isSending)

    // Attempt second submission while first is still sending
    viewModel.onInputTextChanged("Deuxieme envoi")
    viewModel.sendMessage()
    testScheduler.runCurrent()

    assertEquals(1, fakeRepo.callCount)
    advanceUntilIdle()
  }

  @Test
  fun `viewModel image selection and removal update uiState correctly`() = runTest {
    val fakeRepo = FakeAiRepository()
    val useCase = SendMessageUseCase(fakeRepo)
    val conversationRepo = InMemoryConversationRepository()
    val viewModel = ConversationViewModel(conversationRepo, useCase)

    val image = AttachedImage(uri = "content://img/1", base64Data = "base64", mimeType = "image/jpeg")

    viewModel.onImageSelected(image)
    assertEquals(image, viewModel.uiState.value.attachedImage)
    assertEquals(OrbState.FOCUSED, viewModel.uiState.value.orbState)

    viewModel.onRemoveAttachedImage()
    assertNull(viewModel.uiState.value.attachedImage)
    assertEquals(OrbState.IDLE, viewModel.uiState.value.orbState)
  }

  @Test
  fun `geminiAiRepository detects missing or placeholder API key`() = runTest {
    val repoWithEmptyKey = GeminiAiRepository(apiKeyProvider = { "" })
    val result1 = repoWithEmptyKey.generateResponse(Conversation(), "Hello", null)
    assertTrue(result1 is AiResult.Error)
    assertEquals(AiError.ApiKeyMissing, (result1 as AiResult.Error).error)

    val repoWithPlaceholder = GeminiAiRepository(apiKeyProvider = { "MY_GEMINI_API_KEY" })
    val result2 = repoWithPlaceholder.generateResponse(Conversation(), "Hello", null)
    assertTrue(result2 is AiResult.Error)
    assertEquals(AiError.ApiKeyMissing, (result2 as AiResult.Error).error)
  }

  @Test
  fun `conversation preserves chronological order and roles`() = runTest {
    val conversationRepo = InMemoryConversationRepository()
    val msg1 = Message(role = MessageRole.USER, content = "Turn 1")
    val msg2 = Message(role = MessageRole.ASSISTANT, content = "Reply 1")
    val msg3 = Message(role = MessageRole.USER, content = "Turn 2")

    conversationRepo.addMessage(msg1)
    conversationRepo.addMessage(msg2)
    conversationRepo.addMessage(msg3)

    val history = conversationRepo.getCurrentConversation().messages
    assertEquals(3, history.size)
    assertEquals("Turn 1", history[0].content)
    assertEquals(MessageRole.USER, history[0].role)
    assertEquals("Reply 1", history[1].content)
    assertEquals(MessageRole.ASSISTANT, history[1].role)
    assertEquals("Turn 2", history[2].content)
    assertEquals(MessageRole.USER, history[2].role)
  }

  // --- Phase 4 Tests: Styles, Intensity, Weights & Rizz Rapide ---

  @Test
  fun `style settings selection and intensity changes update viewModel uiState`() = runTest {
    val fakeRepo = FakeAiRepository()
    val useCase = SendMessageUseCase(fakeRepo)
    val conversationRepo = InMemoryConversationRepository()
    val viewModel = ConversationViewModel(conversationRepo, useCase)

    assertEquals(ConversationStyle.NEUTRE, viewModel.uiState.value.styleSettings.activeStyle)
    assertEquals(3, viewModel.uiState.value.styleSettings.intensity)

    ConversationStyle.entries.forEach { style ->
      viewModel.selectStyle(style)
      assertEquals(style, viewModel.uiState.value.styleSettings.activeStyle)
      assertFalse(viewModel.uiState.value.styleSettings.isBlendModeEnabled)
    }

    // Intensity 1 to 5
    for (intensity in 1..5) {
      viewModel.setStyleIntensity(intensity)
      assertEquals(intensity, viewModel.uiState.value.styleSettings.intensity)
    }

    // Intensity clamping
    viewModel.setStyleIntensity(10)
    assertEquals(5, viewModel.uiState.value.styleSettings.intensity)
    viewModel.setStyleIntensity(-1)
    assertEquals(1, viewModel.uiState.value.styleSettings.intensity)
  }

  @Test
  fun `style weights strictly normalize and always sum to 100 percent`() {
    val settings = StyleSettings()
    val initialWeights = settings.getNormalizedWeights()
    assertEquals(100, initialWeights.values.sum())

    // Adjust Charo to 60%, others must dynamically scale to sum to exactly 100%
    val adjusted = settings.withAdjustedWeight(ConversationStyle.CHARO, 60)
    assertEquals(60, adjusted.weights[ConversationStyle.CHARO])
    assertEquals(100, adjusted.getNormalizedWeights().values.sum())

    // Adjust Humour to 90%, remaining 10% split among others
    val adjustedHumour = adjusted.withAdjustedWeight(ConversationStyle.HUMOUR, 90)
    assertEquals(90, adjustedHumour.weights[ConversationStyle.HUMOUR])
    assertEquals(100, adjustedHumour.getNormalizedWeights().values.sum())

    // Reset weights
    val reset = adjustedHumour.withResetWeights()
    assertEquals(100, reset.getNormalizedWeights().values.sum())
    assertEquals(40, reset.weights[ConversationStyle.CHARO])
  }

  @Test
  fun `aiConfig buildSystemInstruction injects correct style directives and intensity`() {
    // 1. Single Style Humour Level 5
    val humourSettings = StyleSettings(
      activeStyle = ConversationStyle.HUMOUR,
      intensity = 5,
      isBlendModeEnabled = false
    )
    val humourPrompt = AiConfig.buildSystemInstruction(humourSettings)
    assertTrue(humourPrompt.contains("STYLE ACTIF : HUMOUR"))
    assertTrue(humourPrompt.contains("Niveau 5 (Maximum raisonnable"))

    // 2. Single Style Charo Level 2
    val charoSettings = StyleSettings(
      activeStyle = ConversationStyle.CHARO,
      intensity = 2,
      isBlendModeEnabled = false
    )
    val charoPrompt = AiConfig.buildSystemInstruction(charoSettings)
    assertTrue(charoPrompt.contains("STYLE ACTIF : CHARO"))
    assertTrue(charoPrompt.contains("Niveau 2 (Léger"))

    // 3. Blend Mode
    val blendSettings = StyleSettings(
      isBlendModeEnabled = true,
      intensity = 4,
      weights = mapOf(
        ConversationStyle.CHARO to 50,
        ConversationStyle.HUMOUR to 30,
        ConversationStyle.ROMANTIQUE to 20,
        ConversationStyle.CALCULATEUR to 0
      )
    )
    val blendPrompt = AiConfig.buildSystemInstruction(blendSettings)
    assertTrue(blendPrompt.contains("MODE MÉLANGE DE STYLES PONDÉRÉ"))
    assertTrue(blendPrompt.contains("Charo (50%)"))
    assertTrue(blendPrompt.contains("Humour (30%)"))
    assertTrue(blendPrompt.contains("Romantique (20%)"))
    assertTrue(blendPrompt.contains("Niveau 4 (Prononcé"))
  }

  @Test
  fun `viewModel passes active style settings to AI usecase`() = runTest {
    val fakeRepo = FakeAiRepository()
    val useCase = SendMessageUseCase(fakeRepo)
    val conversationRepo = InMemoryConversationRepository()
    val viewModel = ConversationViewModel(conversationRepo, useCase)
    advanceUntilIdle()

    viewModel.selectStyle(ConversationStyle.CHARO)
    viewModel.setStyleIntensity(4)
    viewModel.onInputTextChanged("Test prompt avec style")
    viewModel.sendMessage()
    advanceUntilIdle()

    val receivedSettings = fakeRepo.lastReceivedStyleSettings
    assertNotNull(receivedSettings)
    assertEquals(ConversationStyle.CHARO, receivedSettings!!.activeStyle)
    assertEquals(4, receivedSettings.intensity)
    assertFalse(receivedSettings.isBlendModeEnabled)
  }

  @Test
  fun `regenerateAssistantMessage finds preceding user prompt and triggers regeneration`() = runTest {
    val fakeRepo = FakeAiRepository().apply {
      resultToReturn = AiResult.Success("Première réponse")
    }
    val useCase = SendMessageUseCase(fakeRepo)
    val conversationRepo = InMemoryConversationRepository()
    val viewModel = ConversationViewModel(conversationRepo, useCase)
    advanceUntilIdle()

    // First interaction
    viewModel.onInputTextChanged("Mon premier message")
    viewModel.sendMessage()
    advanceUntilIdle()

    val messages = conversationRepo.getCurrentConversation().messages
    assertEquals(2, messages.size)
    val assistantId = messages[1].id

    // Now change style to Humour and regenerate assistant message
    viewModel.selectStyle(ConversationStyle.HUMOUR)
    fakeRepo.resultToReturn = AiResult.Success("Réponse régénérée en mode Humour")
    viewModel.regenerateAssistantMessage(assistantId)
    advanceUntilIdle()

    assertEquals(2, fakeRepo.callCount)
    assertEquals("Mon premier message", fakeRepo.lastReceivedPrompt)
    assertEquals(ConversationStyle.HUMOUR, fakeRepo.lastReceivedStyleSettings?.activeStyle)
  }

  @Test
  fun `rizzRapideUseCase validates input and returns 3 proposals`() = runTest {
    val fakeRepo = FakeAiRepository()
    val rizzUseCase = RizzRapideUseCase(fakeRepo)

    // Empty input validation
    val emptyResult = rizzUseCase("   ")
    assertTrue(emptyResult is AiResult.Error)
    assertEquals(0, fakeRepo.rizzRapideCallCount)

    // Valid situation
    val result = rizzUseCase("Elle m'a mis un vu", StyleSettings(activeStyle = ConversationStyle.CHARO))
    assertTrue(result is AiResult.Success)
    val options = (result as AiResult.Success).data
    assertEquals(3, options.size)
    assertEquals("Option 1 : Audacieuse", options[0])
    assertEquals(1, fakeRepo.rizzRapideCallCount)
  }

  @Test
  fun `viewModel Rizz Rapide flow generates and inserts option into chat input`() = runTest {
    val fakeRepo = FakeAiRepository()
    val useCase = SendMessageUseCase(fakeRepo)
    val rizzUseCase = RizzRapideUseCase(fakeRepo)
    val conversationRepo = InMemoryConversationRepository()
    val viewModel = ConversationViewModel(conversationRepo, useCase, rizzUseCase)

    viewModel.openRizzRapide()
    assertTrue(viewModel.uiState.value.isRizzRapideOpen)

    viewModel.updateRizzRapideSituation("Elle demande ce que je fais")
    viewModel.generateRizzRapide()
    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isGeneratingRizzRapide)
    assertEquals(3, viewModel.uiState.value.rizzRapideOptions.size)

    // Use option 2 in main chat input
    viewModel.useRizzRapideOption(viewModel.uiState.value.rizzRapideOptions[1])
    assertFalse(viewModel.uiState.value.isRizzRapideOpen)
    assertEquals("Option 2 : Taquine", viewModel.uiState.value.inputText)
  }
}
