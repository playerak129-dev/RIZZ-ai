package com.example.core

/**
 * Represents an independent block extracted from a long AI response.
 *
 * @param index 1-based index (1, 2, 3...)
 * @param label Display label such as "Bloc 1", "Option 1", etc.
 * @param rawContent Untouched original text of this block (used for clean copy)
 */
data class MessageBlock(
  val index: Int,
  val label: String,
  val rawContent: String
)

/**
 * Result of segmenting an assistant response into logical blocks.
 *
 * @param intro Optional introductory text appearing before the first block (e.g. "Voici 3 options :")
 * @param blocks List of independent blocks
 * @param isSegmented True if the message was segmented into multiple blocks; false for compact single-block display
 */
data class SegmentedMessage(
  val intro: String? = null,
  val blocks: List<MessageBlock>,
  val isSegmented: Boolean
)

object MessageSegmenter {

  /**
   * Intelligently segments an AI assistant response into coherent blocks.
   * Prioritizes numbered options, markdown headings, bulleted lists, and paragraphs.
   *
   * Short responses (< 80 chars without lists) or single-concept texts remain unsegmented
   * for a compact presentation.
   */
  fun segment(content: String): SegmentedMessage {
    if (content.isBlank()) {
      return SegmentedMessage(intro = null, blocks = emptyList(), isSegmented = false)
    }

    val normalized = content.replace("\r\n", "\n").trim()

    // Rule 8: Short responses without structured items stay compact
    if (normalized.length < 80 && !hasStructuredItems(normalized)) {
      return SegmentedMessage(
        intro = null,
        blocks = listOf(MessageBlock(1, "Bloc 1", normalized)),
        isSegmented = false
      )
    }

    // 1. Try segmenting by Markdown Headings (e.g. "### Option 1", "## Approche 2")
    val headingSegmentation = trySegmentByHeadings(normalized)
    if (headingSegmentation != null && headingSegmentation.blocks.size >= 2) {
      return headingSegmentation
    }

    // 2. Try segmenting by Numbered Lists (e.g. "1. ", "2. ", "1° ", "1) ")
    val numberedSegmentation = trySegmentByNumberedList(normalized)
    if (numberedSegmentation != null && numberedSegmentation.blocks.size >= 2) {
      return numberedSegmentation
    }

    // 3. Try segmenting by Bullet Lists (e.g. "• ", "- ", "* ")
    val bulletSegmentation = trySegmentByBullets(normalized)
    if (bulletSegmentation != null && bulletSegmentation.blocks.size >= 2) {
      return bulletSegmentation
    }

    // 4. Try segmenting by Distinct Paragraphs separated by blank lines (\n\n+)
    val paragraphSegmentation = trySegmentByParagraphs(normalized)
    if (paragraphSegmentation != null && paragraphSegmentation.blocks.size >= 2) {
      return paragraphSegmentation
    }

    // Fallback: single block, compact representation
    return SegmentedMessage(
      intro = null,
      blocks = listOf(MessageBlock(1, "Bloc 1", normalized)),
      isSegmented = false
    )
  }

  private fun hasStructuredItems(text: String): Boolean {
    val numberedRegex = Regex("(?m)^[ \\t]*(\\d+)[\\.\\)°][ \\t]+")
    val bulletRegex = Regex("(?m)^[ \\t]*[•\\*\\-][ \\t]+")
    val headingRegex = Regex("(?m)^[ \\t]*#{1,4}[ \\t]+")
    return numberedRegex.findAll(text).count() >= 2 ||
      bulletRegex.findAll(text).count() >= 2 ||
      headingRegex.findAll(text).count() >= 2
  }

  /**
   * Segments by Markdown headings (### or ## at line start).
   */
  private fun trySegmentByHeadings(text: String): SegmentedMessage? {
    val headingRegex = Regex("(?m)^[ \\t]*(#{1,4}[ \\t]+[^\n]+)")
    val matches = headingRegex.findAll(text).toList()
    if (matches.size < 2) return null

    val intro: String? = if (matches.first().range.first > 0) {
      val preText = text.substring(0, matches.first().range.first).trim()
      if (preText.isNotBlank()) preText else null
    } else {
      null
    }

    val blocks = mutableListOf<MessageBlock>()
    for (i in matches.indices) {
      val start = matches[i].range.first
      val end = if (i + 1 < matches.size) matches[i + 1].range.first else text.length
      val blockContent = text.substring(start, end).trim()
      if (blockContent.isNotBlank()) {
        blocks.add(
          MessageBlock(
            index = blocks.size + 1,
            label = "Bloc ${blocks.size + 1}",
            rawContent = blockContent
          )
        )
      }
    }

    return if (blocks.size >= 2) {
      SegmentedMessage(intro = intro, blocks = blocks, isSegmented = true)
    } else {
      null
    }
  }

  /**
   * Segments by Numbered Lists (1. ..., 2. ..., 1° ..., 1) ...).
   */
  private fun trySegmentByNumberedList(text: String): SegmentedMessage? {
    val itemRegex = Regex("(?m)^[ \\t]*(\\d+)[\\.\\)°][ \\t]+")
    val matches = itemRegex.findAll(text).toList()
    if (matches.size < 2) return null

    val intro: String? = if (matches.first().range.first > 0) {
      val preText = text.substring(0, matches.first().range.first).trim()
      if (preText.isNotBlank()) preText else null
    } else {
      null
    }

    val blocks = mutableListOf<MessageBlock>()
    for (i in matches.indices) {
      val start = matches[i].range.first
      val end = if (i + 1 < matches.size) matches[i + 1].range.first else text.length
      val blockContent = text.substring(start, end).trim()
      if (blockContent.isNotBlank()) {
        blocks.add(
          MessageBlock(
            index = blocks.size + 1,
            label = "Bloc ${blocks.size + 1}",
            rawContent = blockContent
          )
        )
      }
    }

    return if (blocks.size >= 2) {
      SegmentedMessage(intro = intro, blocks = blocks, isSegmented = true)
    } else {
      null
    }
  }

  /**
   * Segments by Bullet Lists (•, -, * at start of lines).
   */
  private fun trySegmentByBullets(text: String): SegmentedMessage? {
    val bulletRegex = Regex("(?m)^[ \\t]*[•\\*\\-][ \\t]+")
    val matches = bulletRegex.findAll(text).toList()
    if (matches.size < 2) return null

    val intro: String? = if (matches.first().range.first > 0) {
      val preText = text.substring(0, matches.first().range.first).trim()
      if (preText.isNotBlank()) preText else null
    } else {
      null
    }

    val blocks = mutableListOf<MessageBlock>()
    for (i in matches.indices) {
      val start = matches[i].range.first
      val end = if (i + 1 < matches.size) matches[i + 1].range.first else text.length
      val blockContent = text.substring(start, end).trim()
      if (blockContent.isNotBlank()) {
        blocks.add(
          MessageBlock(
            index = blocks.size + 1,
            label = "Bloc ${blocks.size + 1}",
            rawContent = blockContent
          )
        )
      }
    }

    return if (blocks.size >= 2) {
      SegmentedMessage(intro = intro, blocks = blocks, isSegmented = true)
    } else {
      null
    }
  }

  /**
   * Segments by coherent paragraphs separated by double newlines (\n\n+).
   * Ensures paragraphs are substantial (not single words or orphan lines).
   */
  private fun trySegmentByParagraphs(text: String): SegmentedMessage? {
    val rawParagraphs = text.split(Regex("\n{2,}"))
      .map { it.trim() }
      .filter { it.isNotBlank() }

    // If there are less than 2 paragraphs, or text is short, do not segment
    if (rawParagraphs.size < 2 || text.length < 100) return null

    // Ensure paragraphs are meaningful sentences or content (at least 15 chars)
    val substantialParagraphs = rawParagraphs.filter { it.length >= 15 }
    if (substantialParagraphs.size < 2) return null

    val blocks = rawParagraphs.mapIndexed { index, paragraph ->
      MessageBlock(
        index = index + 1,
        label = "Bloc ${index + 1}",
        rawContent = paragraph
      )
    }

    return SegmentedMessage(intro = null, blocks = blocks, isSegmented = true)
  }
}
