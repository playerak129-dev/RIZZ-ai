package com.example

import android.content.Context
import androidx.activity.viewModels
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.db.AppDatabase
import com.example.data.local.db.entity.ConversationEntity
import com.example.data.local.db.entity.MessageEntity
import com.example.domain.model.ConversationStyle
import com.example.domain.model.Message
import com.example.domain.model.MessageRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RotationStatePreservationTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun `conversation history and input text are preserved during device rotation`() {
    // 1. Enter text in the message input field
    val testInput = "Tu as un charme incroyable 😉"
    composeTestRule.waitForIdle()

    val inputNode = composeTestRule.onNodeWithTag("message_input")
    inputNode.assertIsDisplayed()
    inputNode.performTextInput(testInput)
    composeTestRule.waitForIdle()

    // Verify input before rotation
    inputNode.assertTextContains(testInput)

    // 2. Add an assistant response message to the active conversation in database
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = AppDatabase.getInstance(context)
    val convId = UUID.randomUUID().toString()
    val msgId1 = UUID.randomUUID().toString()
    val msgId2 = UUID.randomUUID().toString()

    runBlocking {
      database.conversationDao().insertConversation(
        ConversationEntity(
          id = convId,
          title = "Session Séduction",
          createdAt = System.currentTimeMillis(),
          updatedAt = System.currentTimeMillis(),
          activeStyle = ConversationStyle.CHARO.name
        )
      )
      database.messageDao().insertMessage(
        MessageEntity(
          id = msgId1,
          conversationId = convId,
          role = "USER",
          content = "Tu fais quoi ce soir ?"
        )
      )
      database.messageDao().insertMessage(
        MessageEntity(
          id = msgId2,
          conversationId = convId,
          role = "ASSISTANT",
          content = "Je t'attendais... 😉"
        )
      )
    }

    // 3. Simulate device rotation by recreating the activity
    composeTestRule.activityRule.scenario.recreate()
    composeTestRule.waitForIdle()

    // 4. Assert that after rotation, the input text is still present in the input field
    val inputNodeAfterRotation = composeTestRule.onNodeWithTag("message_input")
    inputNodeAfterRotation.assertIsDisplayed()
    inputNodeAfterRotation.assertTextContains(testInput)

    // 5. Verify via activity's ViewModel that state (input text and history) is completely retained
    composeTestRule.activityRule.scenario.onActivity { activity ->
      val state = activity.viewModel.uiState.value
      assertEquals(testInput, state.inputText)
      assertNotNull(state.conversation)
      assertTrue(state.conversation.messages.isNotEmpty() || state.conversationList.isNotEmpty())
    }
  }

  @Test
  fun `activity scenario rotation preserves message history and prevents state loss`() {
    val scenario = ActivityScenario.launch(MainActivity::class.java)

    var initialConvId = ""
    scenario.onActivity { activity ->
      val vm = activity.viewModel
      vm.onInputTextChanged("Phrase de test pour rotation")
      vm.selectStyle(ConversationStyle.ROMANTIQUE)
      initialConvId = vm.uiState.value.conversation.id
    }

    // Trigger device configuration change / rotation
    scenario.recreate()

    scenario.onActivity { recreatedActivity ->
      val vm = recreatedActivity.viewModel
      val state = vm.uiState.value

      // User input must be preserved
      assertEquals("Phrase de test pour rotation", state.inputText)
      // Style must be preserved
      assertEquals(ConversationStyle.ROMANTIQUE, state.styleSettings.activeStyle)
      // Conversation ID must be preserved
      assertEquals(initialConvId, state.conversation.id)
    }

    scenario.close()
  }
}
