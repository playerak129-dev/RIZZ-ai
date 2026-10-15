package com.example

import com.example.data.repository.InMemoryConversationRepository
import com.example.domain.model.Message
import com.example.domain.model.MessageRole
import com.example.presentation.components.orb.OrbState
import com.example.presentation.conversation.ConversationViewModel
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationLogicTest {

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `message creation sets role and stable identifier`() {
    val message = Message(
      role = MessageRole.USER,
      content = "Salut RIZZ AI"
    )

    assertNotNull(message.id)
    assertEquals(MessageRole.USER, message.role)
    assertEquals("Salut RIZZ AI", message.content)
    assertTrue(message.timestamp > 0)
  }

  @Test
  fun `all sender roles are uniquely identifiable`() {
    val userMsg = Message(role = MessageRole.USER, content = "Question")
    val aiMsg = Message(role = MessageRole.ASSISTANT, content = "Réponse")
    val sysMsg = Message(role = MessageRole.SYSTEM, content = "Info système")

    assertEquals(MessageRole.USER, userMsg.role)
    assertEquals(MessageRole.ASSISTANT, aiMsg.role)
    assertEquals(MessageRole.SYSTEM, sysMsg.role)
  }

  @Test
  fun `repository adds message and notifies observers`() = runTest {
    val repository = InMemoryConversationRepository()
    val msg = Message(role = MessageRole.USER, content = "Premier test")

    repository.addMessage(msg)
    val current = repository.getCurrentConversation()

    assertEquals(1, current.messages.size)
    assertEquals("Premier test", current.messages.first().content)
  }

  @Test
  fun `repository clears conversation correctly`() = runTest {
    val repository = InMemoryConversationRepository()
    repository.addMessage(Message(role = MessageRole.USER, content = "Message 1"))
    repository.addMessage(Message(role = MessageRole.USER, content = "Message 2"))

    assertEquals(2, repository.getCurrentConversation().messages.size)

    repository.clearConversation()
    assertEquals(0, repository.getCurrentConversation().messages.size)
  }

  @Test
  fun `viewModel input change updates uiState and triggers FOCUSED orbState`() = runTest {
    val repository = InMemoryConversationRepository()
    val viewModel = ConversationViewModel(repository)

    assertEquals(OrbState.IDLE, viewModel.uiState.value.orbState)

    viewModel.onInputTextChanged("Coucou")
    assertEquals("Coucou", viewModel.uiState.value.inputText)
    assertEquals(OrbState.FOCUSED, viewModel.uiState.value.orbState)

    viewModel.onInputTextChanged("")
    assertEquals("", viewModel.uiState.value.inputText)
    assertEquals(OrbState.IDLE, viewModel.uiState.value.orbState)
  }

  @Test
  fun `viewModel sendMessage adds message to conversation clears input and resets orbState`() = runTest {
    val repository = InMemoryConversationRepository()
    val viewModel = ConversationViewModel(repository)
    advanceUntilIdle()

    viewModel.onInputTextChanged("  Message avec espaces  ")
    assertEquals(OrbState.FOCUSED, viewModel.uiState.value.orbState)

    viewModel.sendMessage()
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("", state.inputText)
    assertEquals(OrbState.IDLE, state.orbState)
    assertEquals(1, state.conversation.messages.size)
    val sent = state.conversation.messages.first()
    assertEquals("Message avec espaces", sent.content)
    assertEquals(MessageRole.USER, sent.role)
  }

  @Test
  fun `viewModel blank input does not send message`() = runTest {
    val repository = InMemoryConversationRepository()
    val viewModel = ConversationViewModel(repository)
    advanceUntilIdle()

    viewModel.onInputTextChanged("   ")
    viewModel.sendMessage()
    advanceUntilIdle()

    assertEquals(0, viewModel.uiState.value.conversation.messages.size)
  }

  @Test
  fun `viewModel suggestion selection populates input field and focuses orb`() = runTest {
    val repository = InMemoryConversationRepository()
    val viewModel = ConversationViewModel(repository)

    viewModel.onSelectSuggestion("Idée d'accroche")
    assertEquals("Idée d'accroche", viewModel.uiState.value.inputText)
    assertEquals(OrbState.FOCUSED, viewModel.uiState.value.orbState)
  }

  @Test
  fun `viewModel theme mode cycling cycles SYSTEM to DARK to LIGHT and back`() = runTest {
    val repository = InMemoryConversationRepository()
    val viewModel = ConversationViewModel(repository)

    assertEquals(ThemeMode.SYSTEM, viewModel.uiState.value.themeMode)

    viewModel.cycleThemeMode()
    assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)

    viewModel.cycleThemeMode()
    assertEquals(ThemeMode.LIGHT, viewModel.uiState.value.themeMode)

    viewModel.cycleThemeMode()
    assertEquals(ThemeMode.SYSTEM, viewModel.uiState.value.themeMode)
  }

  @Test
  fun `viewModel direct theme mode change sets mode`() = runTest {
    val repository = InMemoryConversationRepository()
    val viewModel = ConversationViewModel(repository)

    viewModel.setThemeMode(ThemeMode.DARK)
    assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)

    viewModel.setThemeMode(ThemeMode.LIGHT)
    assertEquals(ThemeMode.LIGHT, viewModel.uiState.value.themeMode)
  }

  @Test
  fun `viewModel supports all orb states`() = runTest {
    val repository = InMemoryConversationRepository()
    val viewModel = ConversationViewModel(repository)

    viewModel.setOrbState(OrbState.THINKING)
    assertEquals(OrbState.THINKING, viewModel.uiState.value.orbState)

    viewModel.setOrbState(OrbState.STREAMING)
    assertEquals(OrbState.STREAMING, viewModel.uiState.value.orbState)

    viewModel.setOrbState(OrbState.SUCCESS)
    assertEquals(OrbState.SUCCESS, viewModel.uiState.value.orbState)

    viewModel.setOrbState(OrbState.ERROR)
    assertEquals(OrbState.ERROR, viewModel.uiState.value.orbState)
  }
}
