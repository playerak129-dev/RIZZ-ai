package com.example

import com.example.core.MarkdownFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmojiAndMarkdownFormattingTest {

  // --- 1. Tests des Émojis Unicode ---

  @Test
  fun `emojis are fully preserved in plain text and annotated string`() {
    val textWithEmojis = "Salut toi 😉 ! Toujours partante pour ce soir ? 🔥✨"
    val plain = MarkdownFormatter.cleanForPlainText(textWithEmojis)
    val annotated = MarkdownFormatter.formatToAnnotatedString(textWithEmojis)

    assertEquals(textWithEmojis, plain)
    assertEquals(textWithEmojis, annotated.text)
    assertTrue(plain.contains("😉"))
    assertTrue(plain.contains("🔥"))
    assertTrue(plain.contains("✨"))
  }

  @Test
  fun `multiple complex emojis sequences remain intact`() {
    val complexEmojis = "Option audacieuse 🚀💫❤️‍🔥 et classe 🕶️👌"
    val cleaned = MarkdownFormatter.cleanForPlainText(complexEmojis)
    val annotated = MarkdownFormatter.formatToAnnotatedString(complexEmojis)

    assertEquals(complexEmojis, cleaned)
    assertEquals(complexEmojis, annotated.text)
  }

  @Test
  fun `emojis inside bold markdown retain emoji and apply bold span`() {
    val markdownWithEmoji = "Voici une **réponse piquante 🔥** à lui envoyer."
    val plain = MarkdownFormatter.cleanForPlainText(markdownWithEmoji)
    val annotated = MarkdownFormatter.formatToAnnotatedString(markdownWithEmoji)

    assertEquals("Voici une réponse piquante 🔥 à lui envoyer.", plain)
    assertEquals("Voici une réponse piquante 🔥 à lui envoyer.", annotated.text)
    // Span style was added for the bold portion
    assertTrue(annotated.spanStyles.isNotEmpty())
    assertTrue(annotated.text.contains("🔥"))
  }

  // --- 2. Tests des Accents Français ---

  @Test
  fun `all french lowercase and uppercase accents are strictly preserved`() {
    val allAccents = "Dévoué, élève, fête, événement, maîtresses, août, français, ça va, voilà, où, île. ÉTÉ, À, ÇA, ÎLE."
    val cleaned = MarkdownFormatter.cleanForPlainText(allAccents)
    val annotated = MarkdownFormatter.formatToAnnotatedString(allAccents)

    assertEquals(allAccents, cleaned)
    assertEquals(allAccents, annotated.text)
    // Verify each typical accented character is present
    assertTrue(cleaned.contains("é"))
    assertTrue(cleaned.contains("è"))
    assertTrue(cleaned.contains("ê"))
    assertTrue(cleaned.contains("à"))
    assertTrue(cleaned.contains("ç"))
    assertTrue(cleaned.contains("ù"))
    assertTrue(cleaned.contains("î"))
    assertTrue(cleaned.contains("É"))
    assertTrue(cleaned.contains("À"))
    assertTrue(cleaned.contains("Ç"))
  }

  // --- 3. Tests des Apostrophes et Caractères Spéciaux ---

  @Test
  fun `ascii and typographic apostrophes are preserved`() {
    val text = "C'est l'heure d'y aller, j’espère qu’elle va adorer."
    val cleaned = MarkdownFormatter.cleanForPlainText(text)
    val annotated = MarkdownFormatter.formatToAnnotatedString(text)

    assertEquals(text, cleaned)
    assertEquals(text, annotated.text)
    assertTrue(cleaned.contains("'"))
    assertTrue(cleaned.contains("’"))
  }

  @Test
  fun `special punctuation and quotes are preserved`() {
    val text = "« Tu viens ? » — Absolument ! (100% sûr...)"
    val cleaned = MarkdownFormatter.cleanForPlainText(text)
    val annotated = MarkdownFormatter.formatToAnnotatedString(text)

    assertEquals(text, cleaned)
    assertEquals(text, annotated.text)
    assertTrue(cleaned.contains("«"))
    assertTrue(cleaned.contains("»"))
    assertTrue(cleaned.contains("—"))
    assertTrue(cleaned.contains("100%"))
  }

  // --- 4. Tests des artefacts '****', '°' et backticks ---

  @Test
  fun `four asterisks sequences and divider rows are safely removed`() {
    val textWithDividers = "Voici mon conseil :\n****\nNe sois pas trop pressé 😉\n****\nPrends ton temps."
    val cleaned = MarkdownFormatter.cleanForPlainText(textWithDividers)
    val annotated = MarkdownFormatter.formatToAnnotatedString(textWithDividers)

    assertFalse(cleaned.contains("****"))
    assertFalse(annotated.text.contains("****"))
    assertTrue(cleaned.contains("Ne sois pas trop pressé 😉"))
    assertTrue(cleaned.contains("Prends ton temps."))
  }

  @Test
  fun `inline triple or quadruple asterisks are cleaned without breaking surrounding text`() {
    val text = "Une approche **** direct et efficace !"
    val cleaned = MarkdownFormatter.cleanForPlainText(text)
    val annotated = MarkdownFormatter.formatToAnnotatedString(text)

    assertFalse(cleaned.contains("****"))
    assertFalse(annotated.text.contains("****"))
    assertTrue(cleaned.contains("direct et efficace !"))
  }

  @Test
  fun `backticks around phrases are stripped in plain text and formatted in annotated string`() {
    val text = "Envoie-lui : `Je savais bien que tu n'allais pas résister 😉`"
    val cleaned = MarkdownFormatter.cleanForPlainText(text)
    val annotated = MarkdownFormatter.formatToAnnotatedString(text)

    assertFalse(cleaned.contains("`"))
    assertFalse(annotated.text.contains("`"))
    assertEquals("Envoie-lui : Je savais bien que tu n'allais pas résister 😉", cleaned)
    assertEquals("Envoie-lui : Je savais bien que tu n'allais pas résister 😉", annotated.text)
    // Style applied for code span
    assertTrue(annotated.spanStyles.isNotEmpty())
  }

  @Test
  fun `legitimate degree symbol in temperatures and angles is never corrupted`() {
    val legitimateDegree = "Il fait 20°C dehors et le virage est à 90°."
    val cleaned = MarkdownFormatter.cleanForPlainText(legitimateDegree)
    val annotated = MarkdownFormatter.formatToAnnotatedString(legitimateDegree)

    assertEquals(legitimateDegree, cleaned)
    assertEquals(legitimateDegree, annotated.text)
    assertTrue(cleaned.contains("20°C"))
    assertTrue(cleaned.contains("90°"))
  }

  @Test
  fun `ordinal lists starting with degree like 1° or 2° are normalized cleanly`() {
    val ordinalList = "1° Première option séduction\n2° Deuxième option taquine"
    val cleaned = MarkdownFormatter.cleanForPlainText(ordinalList)
    val annotated = MarkdownFormatter.formatToAnnotatedString(ordinalList)

    assertFalse(cleaned.contains("1°"))
    assertFalse(cleaned.contains("2°"))
    assertTrue(cleaned.contains("1. Première option séduction"))
    assertTrue(cleaned.contains("2. Deuxième option taquine"))

    assertFalse(annotated.text.contains("1°"))
    assertTrue(annotated.text.contains("1. Première option séduction"))
  }

  @Test
  fun `bullet degree at start of line is converted to bullet`() {
    val degreeBullet = "° Option A\n° Option B"
    val cleaned = MarkdownFormatter.cleanForPlainText(degreeBullet)
    val annotated = MarkdownFormatter.formatToAnnotatedString(degreeBullet)

    assertFalse(cleaned.contains("°"))
    assertTrue(cleaned.contains("• Option A"))
    assertTrue(cleaned.contains("• Option B"))

    assertFalse(annotated.text.contains("°"))
    assertTrue(annotated.text.contains("• Option A"))
  }

  @Test
  fun `markdown bold and italic are stripped in plain text for easy copying`() {
    val markdown = "**Top conseil :** Sois *toujours* authentique et confiant."
    val cleaned = MarkdownFormatter.cleanForPlainText(markdown)
    val annotated = MarkdownFormatter.formatToAnnotatedString(markdown)

    assertEquals("Top conseil : Sois toujours authentique et confiant.", cleaned)
    assertEquals("Top conseil : Sois toujours authentique et confiant.", annotated.text)
    // Two styles: bold and italic
    assertEquals(2, annotated.spanStyles.size)
  }
}
