package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.sp
import com.example.core.EmojiDisplayType
import com.example.core.EmojiVisualHelper
import com.example.domain.model.Message
import com.example.domain.model.MessageRole
import com.example.presentation.components.MessageBubble
import com.example.ui.theme.RizzAiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EmojiVisualEnhancementTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  // --- 1. Détection de Jumboji (1 seul émoji, style WhatsApp) ---

  @Test
  fun `single emoji returns JUMBO display type for WhatsApp jumboji effect`() {
    val singleEmojis = listOf("🔥", "😉", "❤️‍🔥", "🌹", "🚀", "✨", "👑", "🫠")

    for (emoji in singleEmojis) {
      assertTrue("Should be only emojis: $emoji", EmojiVisualHelper.isOnlyEmojis(emoji))
      assertEquals("Should have cluster count 1: $emoji", 1, EmojiVisualHelper.countEmojiClusters(emoji))
      assertEquals("Should be JUMBO: $emoji", EmojiDisplayType.JUMBO, EmojiVisualHelper.getDisplayType(emoji))
      assertEquals(38.sp, EmojiVisualHelper.getDisplayType(emoji).fontSize)
      assertEquals(46.sp, EmojiVisualHelper.getDisplayType(emoji).lineHeight)
    }
  }

  // --- 2. Détection des doubles et triples émojis (2 à 3 émojis, style WhatsApp) ---

  @Test
  fun `two or three emojis return LARGE display type`() {
    val multiEmojis = listOf(
      "🔥✨" to 2,
      "😉👌" to 2,
      "🌹❤️‍🔥" to 2,
      "🔥✨💫" to 3,
      "😉❤️‍🔥👌" to 3
    )

    for ((text, expectedCount) in multiEmojis) {
      assertTrue("Should be only emojis: $text", EmojiVisualHelper.isOnlyEmojis(text))
      assertEquals("Count mismatch for $text", expectedCount, EmojiVisualHelper.countEmojiClusters(text))
      assertEquals("Should be LARGE: $text", EmojiDisplayType.LARGE, EmojiVisualHelper.getDisplayType(text))
      assertEquals(28.sp, EmojiVisualHelper.getDisplayType(text).fontSize)
      assertEquals(36.sp, EmojiVisualHelper.getDisplayType(text).lineHeight)
    }
  }

  // --- 3. Détection de 4+ émojis ---

  @Test
  fun `four or more pure emojis return MEDIUM display type`() {
    val text = "🔥✨💫🕶️"
    assertTrue(EmojiVisualHelper.isOnlyEmojis(text))
    assertEquals(4, EmojiVisualHelper.countEmojiClusters(text))
    assertEquals(EmojiDisplayType.MEDIUM, EmojiVisualHelper.getDisplayType(text))
    assertEquals(20.sp, EmojiVisualHelper.getDisplayType(text).fontSize)
    assertEquals(28.sp, EmojiVisualHelper.getDisplayType(text).lineHeight)
  }

  // --- 4. Messages mixtes (texte + émojis) ---

  @Test
  fun `mixed text and emojis returns STANDARD display type with comfortable line-height`() {
    val mixedMessages = listOf(
      "Tu viens ce soir ? 😉",
      "🔥 Rendez-vous à 20h !",
      "Option séduction : sois toi-même ✨",
      "Il fait 20°C dehors ☀️",
      "C'est magnifique ! ❤️‍🔥"
    )

    for (msg in mixedMessages) {
      assertFalse("Should NOT be only emojis: $msg", EmojiVisualHelper.isOnlyEmojis(msg))
      assertEquals("Should be STANDARD: $msg", EmojiDisplayType.STANDARD, EmojiVisualHelper.getDisplayType(msg))
      assertEquals(16.sp, EmojiVisualHelper.getDisplayType(msg).fontSize)
      assertEquals(24.sp, EmojiVisualHelper.getDisplayType(msg).lineHeight)
    }
  }

  // --- 5. Robustesse : Textes sans émoji et symboles spéciaux ---

  @Test
  fun `regular texts and numbers are never identified as only emojis`() {
    val regularTexts = listOf(
      "Bonjour",
      "12345",
      "C'est l'heure d'y aller.",
      "100% sûr !",
      "Option 1 : Charme discret",
      ""
    )

    for (txt in regularTexts) {
      assertFalse("Should not be only emojis: '$txt'", EmojiVisualHelper.isOnlyEmojis(txt))
      assertEquals(EmojiDisplayType.STANDARD, EmojiVisualHelper.getDisplayType(txt))
    }
  }

  // --- 6. Rendu Jetpack Compose dans MessageBubble ---

  @Test
  fun `message bubble renders single emoji and mixed messages without error`() {
    val singleEmojiMessage = Message(
      id = "msg_1",
      role = MessageRole.ASSISTANT,
      content = "😉"
    )

    val mixedMessage = Message(
      id = "msg_2",
      role = MessageRole.ASSISTANT,
      content = "Excellente réplique 🔥 ! Envoie-lui ça."
    )

    composeTestRule.setContent {
      RizzAiTheme {
        MessageBubble(message = singleEmojiMessage)
        MessageBubble(message = mixedMessage)
      }
    }

    composeTestRule.onNodeWithText("😉").assertIsDisplayed()
    composeTestRule.onNodeWithText("Excellente réplique 🔥 ! Envoie-lui ça.").assertIsDisplayed()
  }
}
