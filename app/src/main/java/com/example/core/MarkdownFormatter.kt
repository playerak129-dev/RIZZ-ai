package com.example.core

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object MarkdownFormatter {

  /**
   * Cleans text for plain-text use (e.g., copying to clipboard, inserting into input field).
   * Removes raw markdown markers like bold asterisks (**), backticks (`), stray horizontal dividers (****),
   * while preserving legitimate emojis, French accents, apostrophes, and standard punctuation.
   */
  fun cleanForPlainText(input: String): String {
    if (input.isBlank()) return input

    var text = input

    // 1. Remove divider lines consisting of 3 or more asterisks, dashes, or underscores (e.g. ****, ***, ---)
    text = text.replace(Regex("(?m)^[ \\t]*[\\*\\-_]{3,}[ \\t]*$"), "")

    // 2. Remove inline empty or stray asterisk sequences like **** or ***
    text = text.replace(Regex("\\*{3,}"), "")

    // 3. Remove markdown headers like ### or ## at line start
    text = text.replace(Regex("(?m)^[ \\t]*#{1,6}[ \\t]+"), "")

    // 4. Normalize ordinal bullet lists at start of lines (e.g. "1° " -> "1. ", "2° " -> "2. ")
    // We only match when at the start of a line to preserve legitimate degree symbols like 25°C or 180°
    text = text.replace(Regex("(?m)^[ \\t]*(\\d+)°[ \\t]+"), "$1. ")

    // 5. Normalize degree symbol used as bullet at start of lines (e.g. "° Item" -> "• Item")
    text = text.replace(Regex("(?m)^[ \\t]*°[ \\t]+"), "• ")

    // 6. Normalize raw markdown bullet points (*, -, +) into a clean bullet "• "
    text = text.replace(Regex("(?m)^[ \\t]*[\\*\\-+][ \\t]+"), "• ")

    // 7. Remove bold markdown (**text** or __text__)
    text = text.replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
    text = text.replace(Regex("__(.*?)__"), "$1")

    // 8. Remove italic markdown (*text* or _text_)
    text = text.replace(Regex("(?<!\\w)\\*(.*?)\\*(?!\\w)"), "$1")

    // 9. Remove inline code backticks (`text` -> text)
    text = text.replace(Regex("`([^`\n]+)`"), "$1")

    // 10. Remove multi-line code block fences (``` or ```kotlin)
    text = text.replace(Regex("```[a-zA-Z]*\\n?"), "")

    // 11. Clean up multiple empty lines left by stripped dividers
    text = text.replace(Regex("\n{3,}"), "\n\n")

    return text.trim()
  }

  /**
   * Formats text into an AnnotatedString with Compose styles for Bold, Italic, and Code,
   * without showing raw markdown characters like **, *, backticks, or stray **** dividers.
   * Completely preserves Unicode emojis and French accented characters.
   */
  fun formatToAnnotatedString(
    input: String,
    isUser: Boolean = false,
    isDark: Boolean = true
  ): AnnotatedString {
    if (input.isBlank()) return buildAnnotatedString { append(input) }

    // Pre-clean dividers and bullets before parsing spans
    var preprocessed = input
    // Remove divider lines (****, ***, ---)
    preprocessed = preprocessed.replace(Regex("(?m)^[ \\t]*[\\*\\-_]{3,}[ \\t]*$"), "")
    // Remove inline empty or stray asterisk sequences (****)
    preprocessed = preprocessed.replace(Regex("\\*{3,}"), "")
    // Remove markdown headers like ### or ## at line start
    preprocessed = preprocessed.replace(Regex("(?m)^[ \\t]*#{1,6}[ \\t]+"), "")
    // Convert 1° at line start to 1.
    preprocessed = preprocessed.replace(Regex("(?m)^[ \\t]*(\\d+)°[ \\t]+"), "$1. ")
    // Convert ° as bullet at line start to •
    preprocessed = preprocessed.replace(Regex("(?m)^[ \\t]*°[ \\t]+"), "• ")
    // Convert *, -, + bullets at line start to •
    preprocessed = preprocessed.replace(Regex("(?m)^[ \\t]*[\\*\\-+][ \\t]+"), "• ")
    // Clean up multiple empty lines
    preprocessed = preprocessed.replace(Regex("\n{3,}"), "\n\n").trim()

    return parseInlineMarkdown(preprocessed, isUser, isDark)
  }

  private fun parseInlineMarkdown(
    text: String,
    isUser: Boolean,
    isDark: Boolean
  ): AnnotatedString {
    // Regex matching bold (**...**), inline code (`...`), or italic (*...*)
    val pattern = Regex("(\\*\\*(.+?)\\*\\*)|(`(.+?)`)|(\\*(.+?)\\*)")

    return buildAnnotatedString {
      var lastIndex = 0
      for (match in pattern.findAll(text)) {
        val range = match.range
        if (range.first > lastIndex) {
          append(text.substring(lastIndex, range.first))
        }

        val fullMatch = match.value
        when {
          fullMatch.startsWith("**") && fullMatch.endsWith("**") && fullMatch.length >= 4 -> {
            val content = fullMatch.substring(2, fullMatch.length - 2)
            val start = length
            append(content)
            addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, length)
          }
          fullMatch.startsWith("`") && fullMatch.endsWith("`") && fullMatch.length >= 2 -> {
            val content = fullMatch.substring(1, fullMatch.length - 1)
            val start = length
            append(content)
            val codeBg = if (isUser) {
              Color.White.copy(alpha = 0.2f)
            } else if (isDark) {
              Color(0xFF334155).copy(alpha = 0.5f)
            } else {
              Color(0xFFE2E8F0)
            }
            addStyle(
              SpanStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                background = codeBg
              ),
              start,
              length
            )
          }
          fullMatch.startsWith("*") && fullMatch.endsWith("*") && fullMatch.length >= 2 -> {
            val content = fullMatch.substring(1, fullMatch.length - 1)
            val start = length
            append(content)
            addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, length)
          }
          else -> {
            append(fullMatch)
          }
        }
        lastIndex = range.last + 1
      }

      if (lastIndex < text.length) {
        append(text.substring(lastIndex))
      }
    }
  }
}
