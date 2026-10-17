package com.example.presentation.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Input
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.DateTimeUtils
import com.example.core.EmojiVisualHelper
import com.example.core.MarkdownFormatter
import com.example.core.MessageBlock
import com.example.core.MessageSegmenter
import com.example.domain.model.Message
import com.example.domain.model.MessageRole
import com.example.presentation.components.glass.liquidGlass
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.LocalThemeIsDark
import com.example.ui.theme.UserBubbleGradientEnd
import com.example.ui.theme.UserBubbleGradientStart
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
  message: Message,
  onCopyClicked: ((String) -> Unit)? = null,
  onRegenerateClicked: ((String) -> Unit)? = null,
  onReuseClicked: ((String) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val isUser = message.role == MessageRole.USER
  val isDark = LocalThemeIsDark.current
  val context = LocalContext.current
  var isCopied by remember { mutableStateOf(false) }

  // Auto reset copy confirmation state after 2 seconds
  LaunchedEffect(isCopied) {
    if (isCopied) {
      delay(2000)
      isCopied = false
    }
  }

  val alignment = if (isUser) Alignment.End else Alignment.Start
  val bubbleShape = if (isUser) {
    RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
  } else {
    RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 6.dp),
    horizontalAlignment = alignment
  ) {
    // Header (Role label & timestamp)
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
      modifier = Modifier.padding(bottom = 4.dp, start = 4.dp, end = 4.dp)
    ) {
      Text(
        text = when (message.role) {
          MessageRole.USER -> stringResource(R.string.user_role)
          MessageRole.ASSISTANT -> stringResource(R.string.assistant_role)
          MessageRole.SYSTEM -> stringResource(R.string.system_role)
        },
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
      )
      Text(
        text = " • ${DateTimeUtils.formatTime(message.timestamp)}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
      )
    }

    // Message Bubble Surface (clickable / long-clickable for copy)
    Box(
      modifier = Modifier
        .widthIn(max = 340.dp)
        .then(
          if (isUser) {
            Modifier
              .clip(bubbleShape)
              .background(
                Brush.linearGradient(
                  colors = listOf(UserBubbleGradientStart, UserBubbleGradientEnd)
                )
              )
          } else {
            Modifier.liquidGlass(
              shape = bubbleShape,
              isElevated = false,
              borderWidth = 1.dp
            )
          }
        )
        .combinedClickable(
          onClick = { /* normal click */ },
          onLongClick = {
            if (message.content.isNotBlank() && onCopyClicked != null) {
              isCopied = true
              onCopyClicked(MarkdownFormatter.cleanForPlainText(message.content))
            }
          }
        )
        .padding(12.dp)
        .testTag("message_bubble_${message.id}")
    ) {
      Column {
        // Display attached image if present
        if (message.imageUri != null) {
          val bitmap = remember(message.imageUri) {
            try {
              context.contentResolver.openInputStream(Uri.parse(message.imageUri))?.use {
                BitmapFactory.decodeStream(it)
              }
            } catch (e: Exception) {
              null
            }
          }

          if (bitmap != null) {
            Image(
              bitmap = bitmap.asImageBitmap(),
              contentDescription = "Photo jointe",
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp)
                .clip(RoundedCornerShape(12.dp))
            )
            if (message.content.isNotBlank()) {
              Spacer(modifier = Modifier.height(8.dp))
            }
          }
        }

        if (message.content.isNotBlank()) {
          val isAssistant = !isUser && message.role == MessageRole.ASSISTANT
          val segmented = remember(message.content, isAssistant) {
            if (isAssistant) MessageSegmenter.segment(message.content) else null
          }

          if (segmented != null && segmented.isSegmented && segmented.blocks.size > 1) {
            // Segmented presentation for long AI responses
            Column(modifier = Modifier.fillMaxWidth()) {
              if (!segmented.intro.isNullOrBlank()) {
                val formattedIntro = remember(segmented.intro, isDark) {
                  MarkdownFormatter.formatToAnnotatedString(
                    input = segmented.intro,
                    isUser = false,
                    isDark = isDark
                  )
                }
                Text(
                  text = formattedIntro,
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    lineHeight = 22.sp
                  ),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier
                    .padding(bottom = 8.dp)
                    .testTag("message_intro_${message.id}")
                )
              }

              Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                segmented.blocks.forEach { block ->
                  MessageBlockItem(
                    block = block,
                    messageId = message.id,
                    isDark = isDark,
                    onCopyClicked = onCopyClicked
                  )
                }
              }
            }
          } else {
            // Compact presentation for short responses, single block, or user messages
            val formattedContent = remember(message.content, isUser, isDark) {
              MarkdownFormatter.formatToAnnotatedString(
                input = message.content,
                isUser = isUser,
                isDark = isDark
              )
            }
            val emojiDisplayType = remember(message.content) {
              EmojiVisualHelper.getDisplayType(message.content)
            }
            Text(
              text = formattedContent,
              style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = emojiDisplayType.fontSize,
                lineHeight = emojiDisplayType.lineHeight
              ),
              color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }
    }

    // Assistant Action Bar (Copier, Régénérer, Réutiliser)
    if (!isUser && message.role == MessageRole.ASSISTANT && message.content.isNotBlank()) {
      Row(
        modifier = Modifier.padding(top = 2.dp, start = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Copy to clipboard action button
        if (onCopyClicked != null) {
          IconButton(
            onClick = {
              isCopied = true
              onCopyClicked(MarkdownFormatter.cleanForPlainText(message.content))
            },
            modifier = Modifier
              .size(44.dp)
              .testTag("copy_message_${message.id}")
          ) {
            Icon(
              imageVector = if (isCopied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
              contentDescription = "Copier le message dans le presse-papier",
              tint = if (isCopied) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
              modifier = Modifier.size(18.dp)
            )
          }

          AnimatedVisibility(
            visible = isCopied,
            enter = fadeIn(),
            exit = fadeOut()
          ) {
            Text(
              text = "Copié !",
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
              ),
              color = Color(0xFF10B981),
              modifier = Modifier
                .padding(end = 4.dp)
                .testTag("copied_indicator_${message.id}")
            )
          }
        }

        // Regenerate response action button
        if (onRegenerateClicked != null) {
          IconButton(
            onClick = { onRegenerateClicked(message.id) },
            modifier = Modifier
              .size(44.dp)
              .testTag("regenerate_message_${message.id}")
          ) {
            Icon(
              imageVector = Icons.Outlined.Refresh,
              contentDescription = "Régénérer cette réponse",
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
              modifier = Modifier.size(18.dp)
            )
          }
        }

        // Reuse response action button
        if (onReuseClicked != null) {
          IconButton(
            onClick = { onReuseClicked(MarkdownFormatter.cleanForPlainText(message.content)) },
            modifier = Modifier
              .size(44.dp)
              .testTag("reuse_message_${message.id}")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.Input,
              contentDescription = "Réutiliser cette réponse",
              tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(2.dp))
  }
}

/**
 * Renders an independent block of an AI response with its own dedicated Copy button,
 * subtle Liquid Glass / card styling, and copy confirmation feedback.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBlockItem(
  block: MessageBlock,
  messageId: String,
  isDark: Boolean,
  onCopyClicked: ((String) -> Unit)?,
  modifier: Modifier = Modifier
) {
  var isBlockCopied by remember { mutableStateOf(false) }

  LaunchedEffect(isBlockCopied) {
    if (isBlockCopied) {
      delay(2000)
      isBlockCopied = false
    }
  }

  val formattedContent = remember(block.rawContent, isDark) {
    MarkdownFormatter.formatToAnnotatedString(
      input = block.rawContent,
      isUser = false,
      isDark = isDark
    )
  }

  val emojiDisplayType = remember(block.rawContent) {
    EmojiVisualHelper.getDisplayType(block.rawContent)
  }

  val blockBgColor = if (isDark) {
    Color(0xFF1E293B).copy(alpha = 0.55f)
  } else {
    Color(0xFFF8FAFC).copy(alpha = 0.9f)
  }

  val blockBorderColor = if (isDark) {
    Color(0xFF334155).copy(alpha = 0.6f)
  } else {
    Color(0xFFCBD5E1)
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(blockBgColor)
      .border(0.5.dp, blockBorderColor, RoundedCornerShape(12.dp))
      .combinedClickable(
        onClick = { /* normal tap */ },
        onLongClick = {
          if (block.rawContent.isNotBlank() && onCopyClicked != null) {
            isBlockCopied = true
            onCopyClicked(MarkdownFormatter.cleanForPlainText(block.rawContent))
          }
        }
      )
      .padding(10.dp)
      .testTag("message_block_${messageId}_${block.index}")
  ) {
    // Header row: Badge "Bloc X"
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 6.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
          .padding(horizontal = 7.dp, vertical = 2.dp)
      ) {
        Text(
          text = block.label,
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
          ),
          color = MaterialTheme.colorScheme.primary
        )
      }
    }

    // Block Content
    val blockFontSize = if (emojiDisplayType.fontSize.value > 16f) 16.sp else emojiDisplayType.fontSize
    val blockLineHeight = if (emojiDisplayType.lineHeight.value > 24f) 24.sp else emojiDisplayType.lineHeight

    Text(
      text = formattedContent,
      style = MaterialTheme.typography.bodyMedium.copy(
        fontSize = blockFontSize,
        lineHeight = blockLineHeight
      ),
      color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(6.dp))

    // Block Bottom Row: Dedicated Copy Button for this specific block
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.End,
      verticalAlignment = Alignment.CenterVertically
    ) {
      AnimatedVisibility(
        visible = isBlockCopied,
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        Text(
          text = "Copié !",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
          ),
          color = Color(0xFF10B981),
          modifier = Modifier
            .padding(end = 6.dp)
            .testTag("copied_block_indicator_${messageId}_${block.index}")
        )
      }

      Surface(
        onClick = {
          if (block.rawContent.isNotBlank() && onCopyClicked != null) {
            isBlockCopied = true
            onCopyClicked(MarkdownFormatter.cleanForPlainText(block.rawContent))
          }
        },
        shape = RoundedCornerShape(8.dp),
        color = if (isBlockCopied) {
          Color(0xFF10B981).copy(alpha = 0.15f)
        } else if (isDark) {
          Color(0xFF334155).copy(alpha = 0.45f)
        } else {
          Color(0xFFE2E8F0)
        },
        border = BorderStroke(
          0.5.dp,
          if (isBlockCopied) Color(0xFF10B981) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
          .height(30.dp)
          .testTag("copy_block_${messageId}_${block.index}")
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = if (isBlockCopied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
            contentDescription = "Copier ce bloc",
            tint = if (isBlockCopied) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(13.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = if (isBlockCopied) "Copié" else "Copier",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.SemiBold,
              fontSize = 11.sp
            ),
            color = if (isBlockCopied) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
          )
        }
      }
    }
  }
}
