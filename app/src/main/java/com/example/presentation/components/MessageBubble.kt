package com.example.presentation.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Input
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.domain.model.Message
import com.example.domain.model.MessageRole
import com.example.presentation.components.glass.liquidGlass
import com.example.ui.theme.LocalThemeIsDark
import com.example.ui.theme.UserBubbleGradientEnd
import com.example.ui.theme.UserBubbleGradientStart

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

    // Message Bubble Surface
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
          Text(
            text = message.content,
            style = MaterialTheme.typography.bodyLarge.copy(
              lineHeight = 22.sp
            ),
            color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }

    // Assistant Action Bar (Copier & Régénérer)
    if (!isUser && message.role == MessageRole.ASSISTANT && message.content.isNotBlank()) {
      Row(
        modifier = Modifier.padding(top = 2.dp, start = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (onCopyClicked != null) {
          IconButton(
            onClick = { onCopyClicked(message.content) },
            modifier = Modifier
              .size(36.dp)
              .testTag("copy_message_${message.id}")
          ) {
            Icon(
              imageVector = Icons.Outlined.ContentCopy,
              contentDescription = "Copier la réponse",
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
              modifier = Modifier.size(16.dp)
            )
          }
        }

        if (onRegenerateClicked != null) {
          IconButton(
            onClick = { onRegenerateClicked(message.id) },
            modifier = Modifier
              .size(36.dp)
              .testTag("regenerate_message_${message.id}")
          ) {
            Icon(
              imageVector = Icons.Outlined.Refresh,
              contentDescription = "Régénérer cette réponse",
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
              modifier = Modifier.size(16.dp)
            )
          }
        }

        if (onReuseClicked != null) {
          IconButton(
            onClick = { onReuseClicked(message.content) },
            modifier = Modifier
              .size(36.dp)
              .testTag("reuse_message_${message.id}")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.Input,
              contentDescription = "Réutiliser cette réponse",
              tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(2.dp))
  }
}
