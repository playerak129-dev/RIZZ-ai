package com.example

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.core.MarkdownFormatter
import com.example.core.MessageSegmenter
import com.example.domain.model.Message
import com.example.domain.model.MessageRole
import com.example.presentation.components.MessageBubble
import com.example.ui.theme.RizzAiTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SegmentedResponseAndCopyTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // =========================================================================
    // 1. Tests unitaires de segmentation intelligente (MessageSegmenter)
    // =========================================================================

    @Test
    fun `short messages are not segmented and stay compact`() {
        val shortTexts = listOf(
            "Salut ! Ça te dirait qu'on aille boire un verre ce jeudi ? 😉",
            "Très bonne réponse ! Envoie-lui ça sans hésiter 🔥",
            "Attends quelques heures avant de relancer. ⏳"
        )

        for (text in shortTexts) {
            val result = MessageSegmenter.segment(text)

            assertFalse(
                "Short message should not be segmented: $text",
                result.isSegmented
            )
            assertEquals("Should have exactly 1 block", 1, result.blocks.size)
            assertEquals("Content must match", text, result.blocks.first().rawContent)
            assertNull("No intro expected", result.intro)
        }
    }

    @Test
    fun `numbered options are segmented into independent blocks with intro extracted`() {
        val numberedResponse = """
            Voici 3 approches possibles pour lui répondre :

            1. **Le joueur audacieux** : Tu as toujours d'aussi bons goûts ou c'est un coup de chance ? 😏
            2. **Le second degré** : J'allais te dire la même chose, arrête de copier mes pensées ! ✨
            3. **La proposition directe** : Vendredi 20h, je t'emmène au meilleur bar de la ville. 🍸
        """.trimIndent()

        val result = MessageSegmenter.segment(numberedResponse)

        assertTrue("Should be segmented", result.isSegmented)
        assertEquals("Should extract 3 blocks", 3, result.blocks.size)
        assertNotNull("Intro should be extracted", result.intro)
        assertTrue(
            "Intro content mismatch",
            result.intro!!.contains("Voici 3 approches possibles")
        )

        assertTrue(
            "Block 1 content",
            result.blocks[0].rawContent.contains("Le joueur audacieux")
        )
        assertTrue(
            "Block 2 content",
            result.blocks[1].rawContent.contains("Le second degré")
        )
        assertTrue(
            "Block 3 content",
            result.blocks[2].rawContent.contains("La proposition directe")
        )

        assertEquals("Bloc 1", result.blocks[0].label)
        assertEquals("Bloc 2", result.blocks[1].label)
        assertEquals("Bloc 3", result.blocks[2].label)
    }

    @Test
    fun `markdown headings are segmented into coherent sections`() {
        val headingResponse = """
            ### 1. Approche Compliment
            Ton énergie sur cette photo est incroyable, tu rayonnes ! ✨

            ### 2. Approche Mystérieuse
            J'ai l'impression qu'on s'est déjà croisés quelque part... Coïncidence ? 🕵️‍♂️

            ### 3. Approche Humour
            Si tu cuisines aussi bien que tu choisis tes restos, je valide direct 🍕
        """.trimIndent()

        val result = MessageSegmenter.segment(headingResponse)

        assertTrue("Should be segmented", result.isSegmented)
        assertEquals(3, result.blocks.size)
        assertTrue(result.blocks[0].rawContent.contains("Approche Compliment"))
        assertTrue(result.blocks[1].rawContent.contains("Approche Mystérieuse"))
        assertTrue(result.blocks[2].rawContent.contains("Approche Humour"))
    }

    @Test
    fun `bullet list items are segmented when response is sufficiently long`() {
        val bulletResponse = """
            Voici quelques pistes pour relancer la conversation avec style :
            • Tu préfères un cocktail branché ou un rooftop avec vue ? 🍹
            • Je parie que tu n'as pas réussi à finir ce marathon... 😏
            • Dis-moi ton top 3 des endroits à visiter cette année ! ✈️
        """.trimIndent()

        val result = MessageSegmenter.segment(bulletResponse)

        assertTrue("Should be segmented", result.isSegmented)
        assertEquals(3, result.blocks.size)
        assertTrue(result.blocks[0].rawContent.contains("Tu préfères un cocktail"))
        assertTrue(result.blocks[1].rawContent.contains("Je parie que tu n'as pas"))
        assertTrue(result.blocks[2].rawContent.contains("Dis-moi ton top 3"))
    }

    @Test
    fun `multi-paragraph responses are segmented without splitting sentences`() {
        val paragraphResponse = """
            Salut Sophie ! J'ai vu ta story sur le festival, l'ambiance avait l'air incroyable. Tu y vas chaque année ? 🎶

            De mon côté j'ai reconnu l'un des DJs qui passait, sa sélection était vraiment impressionnante ! Si jamais tu as d'autres recommandations dans ce genre, je suis preneur. 🎧
        """.trimIndent()

        val result = MessageSegmenter.segment(paragraphResponse)

        assertTrue("Should be segmented", result.isSegmented)
        assertEquals(2, result.blocks.size)
        assertTrue(result.blocks[0].rawContent.contains("Salut Sophie"))
        assertTrue(result.blocks[1].rawContent.contains("De mon côté j'ai reconnu"))
    }

    @Test
    fun `cleaned text for each block preserves emojis, accents, and removes markdown artifacts`() {
        val rawBlock =
            "1. **Option Audacieuse** : Vendredi 20h, c'est toi qui choisis l'endroit ? 😉"

        val cleaned = MarkdownFormatter.cleanForPlainText(rawBlock)

        assertEquals(
            "1. Option Audacieuse : Vendredi 20h, c'est toi qui choisis l'endroit ? 😉",
            cleaned
        )
        assertFalse(cleaned.contains("**"))
        assertTrue(cleaned.contains("choisis l'endroit"))
        assertTrue(cleaned.contains("😉"))
        assertFalse(cleaned.contains("Bloc 1"))
        assertFalse(cleaned.contains("Copier"))
    }

    // =========================================================================
    // 2. Tests d'interface Robolectric : Copie indépendante par bloc
    // =========================================================================

    @Test
    fun `each block copy button copies only its own content independently`() {
        val assistantMessage = Message(
            id = "msg_segmented_1",
            role = MessageRole.ASSISTANT,
            content = """
                Voici 2 choix :

                1. **Premier choix** : Tu es plutôt team café ou thé ? ☕
                2. **Deuxième choix** : Ton chien a l'air plus sociable que toi 😉
            """.trimIndent()
        )

        var copiedText: String? = null

        composeTestRule.setContent {
            RizzAiTheme {
                MessageBubble(
                    message = assistantMessage,
                    onCopyClicked = { text -> copiedText = text }
                )
            }
        }

        composeTestRule
            .onNodeWithTag("copy_block_msg_segmented_1_1")
            .assertExists()

        composeTestRule
            .onNodeWithTag("copy_block_msg_segmented_1_2")
            .assertExists()

        // Copier uniquement le Bloc 2.
        composeTestRule
            .onNodeWithTag("copy_block_msg_segmented_1_2")
            .performClick()

        assertNotNull("Copied text should not be null", copiedText)
        assertTrue("Should copy only Block 2", copiedText!!.contains("Deuxième choix"))
        assertTrue("Should contain emoji of Block 2", copiedText!!.contains("😉"))
        assertFalse("Must NOT contain Block 1", copiedText!!.contains("Premier choix"))
        assertFalse("Must NOT contain UI title", copiedText!!.contains("Bloc 2"))
        assertFalse("Must NOT contain button label", copiedText!!.contains("Copier"))

        // Copier uniquement le Bloc 1.
        composeTestRule
            .onNodeWithTag("copy_block_msg_segmented_1_1")
            .performClick()

        assertTrue("Should copy only Block 1", copiedText!!.contains("Premier choix"))
        assertTrue("Should contain emoji of Block 1", copiedText!!.contains("☕"))
        assertFalse("Must NOT contain Block 2", copiedText!!.contains("Deuxième choix"))

        // Copier la réponse complète.
        composeTestRule
            .onNodeWithTag("copy_message_msg_segmented_1")
            .performClick()

        assertTrue("Should contain Block 1", copiedText!!.contains("Premier choix"))
        assertTrue("Should contain Block 2", copiedText!!.contains("Deuxième choix"))

        composeTestRule.mainClock.advanceTimeBy(2500)
    }

    @Test
    fun `short assistant message is not artificially segmented and has global copy button`() {
        val shortMessage = Message(
            id = "msg_short_1",
            role = MessageRole.ASSISTANT,
            content = "Je te conseille d'attendre demain matin pour lui écrire 😉."
        )

        var copiedText: String? = null

        composeTestRule.setContent {
            RizzAiTheme {
                MessageBubble(
                    message = shortMessage,
                    onCopyClicked = { text -> copiedText = text }
                )
            }
        }

        // Le message court ne doit pas avoir de sous-blocs artificiels.
        composeTestRule
            .onNodeWithTag("copy_block_msg_short_1_1")
            .assertDoesNotExist()

        // Le bouton de copie globale doit être présent.
        composeTestRule
            .onNodeWithTag("copy_message_msg_short_1")
            .assertIsDisplayed()

        // Copier le message complet.
        composeTestRule
            .onNodeWithTag("copy_message_msg_short_1")
            .performClick()

        assertEquals(
            "Je te conseille d'attendre demain matin pour lui écrire 😉.",
            copiedText
        )

        composeTestRule.mainClock.advanceTimeBy(2500)
    }

    @Test
    fun `user messages are never segmented into blocks`() {
        val userMessage = Message(
            id = "user_msg_1",
            role = MessageRole.USER,
            content = "1. Premier message\n2. Deuxième message\n3. Troisième message"
        )

        composeTestRule.setContent {
            RizzAiTheme {
                MessageBubble(message = userMessage)
            }
        }

        // Aucun bloc ou bouton de sous-copie pour l'utilisateur.
        composeTestRule
            .onNodeWithTag("copy_block_user_msg_1_1")
            .assertDoesNotExist()

        composeTestRule
            .onNodeWithTag("copy_message_user_msg_1")
            .assertDoesNotExist()
    }
}