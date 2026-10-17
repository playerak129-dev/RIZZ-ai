package com.example.core

import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Visual display categories inspired by modern messaging apps (WhatsApp / Telegram):
 * - JUMBO: 1 single standalone emoji displayed large and vibrant
 * - LARGE: 2 to 3 standalone emojis displayed prominently
 * - MEDIUM: 4 to 8 standalone emojis
 * - STANDARD: Standard body text size with comfortable lineHeight to prevent emoji clipping
 */
enum class EmojiDisplayType(
  val fontSize: TextUnit,
  val lineHeight: TextUnit
) {
  JUMBO(38.sp, 46.sp),
  LARGE(28.sp, 36.sp),
  MEDIUM(20.sp, 28.sp),
  STANDARD(16.sp, 24.sp)
}

object EmojiVisualHelper {

  /**
   * Identifies whether a Unicode codepoint belongs to emoji ranges or emoji modifier/presentation selectors.
   */
  fun isEmojiCodePoint(codePoint: Int): Boolean {
    return when (codePoint) {
      in 0x1F600..0x1F64F -> true // Emoticons (😀, 😂, 😉, etc.)
      in 0x1F300..0x1F5FF -> true // Misc Symbols & Pictographs (🔥, 🌹, 💫, 🕶️, etc.)
      in 0x1F680..0x1F6FF -> true // Transport & Map (🚀, 🚗, etc.)
      in 0x1F900..0x1F9FF -> true // Supplemental Symbols & Pictographs (🫠, 🥹, 🫡, etc.)
      in 0x1FA70..0x1FAFF -> true // Symbols & Pictographs Extended-A (🪩, 🪄, etc.)
      in 0x1FA00..0x1FA6F -> true // Chess & game pieces (♟️, 👑, etc.)
      in 0x2600..0x26FF -> true   // Misc Symbols (⚡, ☕, ⚠️, ⚓, ♟, etc.)
      in 0x2700..0x27BF -> true   // Dingbats (✨, ❤️, ✈️, ✉️, etc.)
      in 0x1F1E6..0x1F1FF -> true // Regional Indicator Symbols (Flags)
      in 0x1F3FB..0x1F3FF -> true // Skin tone modifiers
      in 0xFE00..0xFE0F -> true   // Variation selectors (e.g. \uFE0F for color emoji presentation)
      0x200D -> true              // Zero Width Joiner (ZWJ, e.g. ❤️ + ZWJ + 🔥 = ❤️‍🔥)
      0x20E3 -> true              // Combining enclosing keycap
      0x2B50, 0x2B55 -> true      // ⭐, ⭕
      0x231A, 0x231B, 0x23F0, 0x23F3 -> true // ⌚, ⌛, ⏰, ⏳
      in 0x2194..0x21AA -> true      // Arrow symbols used as emoji
      0x25AA, 0x25AB -> true         // Small squares
      in 0x25FB..0x25FE -> true      // Medium squares
      else -> false
    }
  }

  /**
   * Returns true if the string is non-blank and consists EXCLUSIVELY of emojis and whitespace.
   */
  fun isOnlyEmojis(text: String): Boolean {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return false

    var i = 0
    val len = trimmed.length
    var emojiCount = 0

    while (i < len) {
      val cp = Character.codePointAt(trimmed, i)
      val charCount = Character.charCount(cp)

      if (Character.isWhitespace(cp)) {
        i += charCount
        continue
      }

      if (!isEmojiCodePoint(cp)) {
        return false
      }

      emojiCount++
      i += charCount
    }

    return emojiCount > 0
  }

  /**
   * Counts the number of distinct emoji clusters (respecting ZWJ sequences, skin tones, and variation selectors).
   * Returns -1 if the string contains non-emoji characters.
   */
  fun countEmojiClusters(text: String): Int {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return 0

    var clusterCount = 0
    var i = 0
    val len = trimmed.length
    var previousWasZwj = false

    while (i < len) {
      val cp = Character.codePointAt(trimmed, i)
      val charCount = Character.charCount(cp)

      if (Character.isWhitespace(cp)) {
        previousWasZwj = false
        i += charCount
        continue
      }

      if (isEmojiCodePoint(cp)) {
        when {
          cp == 0x200D -> {
            // Zero Width Joiner: bonds with the following emoji in the same cluster
            previousWasZwj = true
          }
          cp in 0xFE00..0xFE0F || cp in 0x1F3FB..0x1F3FF || cp == 0x20E3 -> {
            // Variation selector, skin tone, or keycap modifier
            previousWasZwj = false
          }
          else -> {
            // Base emoji codepoint
            if (previousWasZwj) {
              // Continuation of cluster via ZWJ
              previousWasZwj = false
            } else {
              // New distinct emoji cluster
              clusterCount++
            }
          }
        }
      } else {
        // Contains regular character
        return -1
      }

      i += charCount
    }

    return clusterCount
  }

  /**
   * Resolves the visual display type for a given message content.
   */
  fun getDisplayType(text: String): EmojiDisplayType {
    val cleaned = MarkdownFormatter.cleanForPlainText(text)
    if (!isOnlyEmojis(cleaned)) {
      return EmojiDisplayType.STANDARD
    }

    val count = countEmojiClusters(cleaned)
    return when (count) {
      1 -> EmojiDisplayType.JUMBO
      2, 3 -> EmojiDisplayType.LARGE
      in 4..8 -> EmojiDisplayType.MEDIUM
      else -> EmojiDisplayType.STANDARD
    }
  }
}
