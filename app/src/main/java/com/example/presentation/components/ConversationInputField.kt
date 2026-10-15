package com.example.presentation.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.ImageCompressor
import com.example.domain.model.AttachedImage
import com.example.presentation.components.glass.liquidGlass
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.LocalThemeIsDark
import com.example.ui.theme.MysticViolet
import kotlinx.coroutines.launch

@Composable
fun ConversationInputField(
  inputText: String,
  attachedImage: AttachedImage?,
  isSending: Boolean,
  onTextChanged: (String) -> Unit,
  onImageAttached: (AttachedImage?) -> Unit,
  onRemoveImage: () -> Unit,
  onSendClicked: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val isDark = LocalThemeIsDark.current
  val isSendEnabled = (inputText.isNotBlank() || attachedImage != null) && !isSending

  // Zero-permission Android Photo Picker
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      coroutineScope.launch {
        val processed = ImageCompressor.processImageUri(context, uri)
        onImageAttached(processed)
      }
    }
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .navigationBarsPadding()
      .padding(horizontal = 16.dp, vertical = 8.dp)
  ) {
    // Attached image preview banner
    AnimatedVisibility(
      visible = attachedImage != null,
      enter = fadeIn() + slideInVertically { it },
      exit = fadeOut() + slideOutVertically { it }
    ) {
      if (attachedImage != null) {
        val previewBitmap = remember(attachedImage.uri) {
          try {
            context.contentResolver.openInputStream(Uri.parse(attachedImage.uri))?.use {
              BitmapFactory.decodeStream(it)
            }
          } catch (e: Exception) {
            null
          }
        }

        Box(
          modifier = Modifier
            .padding(bottom = 8.dp, start = 8.dp)
            .size(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
              width = 1.5.dp,
              brush = Brush.linearGradient(listOf(ElectricBlue, MysticViolet)),
              shape = RoundedCornerShape(16.dp)
            )
            .background(Color.Black.copy(alpha = 0.5f))
        ) {
          if (previewBitmap != null) {
            Image(
              bitmap = previewBitmap.asImageBitmap(),
              contentDescription = stringResource(R.string.attach_image_desc),
              contentScale = ContentScale.Crop,
              modifier = Modifier.matchParentSize()
            )
          }

          // Remove image button
          IconButton(
            onClick = onRemoveImage,
            modifier = Modifier
              .align(Alignment.TopEnd)
              .size(26.dp)
              .background(Color.Black.copy(alpha = 0.7f), CircleShape)
              .testTag("remove_image_button")
          ) {
            Icon(
              imageVector = Icons.Outlined.Close,
              contentDescription = stringResource(R.string.remove_image_desc),
              tint = Color.White,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }
    }

    // Main Liquid Glass input container
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .liquidGlass(
          shape = RoundedCornerShape(26.dp),
          isElevated = true,
          borderWidth = 1.dp
        )
        .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Photo attachment button
        IconButton(
          onClick = {
            photoPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
          enabled = !isSending,
          modifier = Modifier
            .size(48.dp)
            .testTag("attach_image_button")
        ) {
          Icon(
            imageVector = Icons.Outlined.AddPhotoAlternate,
            contentDescription = stringResource(R.string.attach_image_desc),
            tint = if (attachedImage != null) {
              MaterialTheme.colorScheme.primary
            } else {
              MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            },
            modifier = Modifier.size(24.dp)
          )
        }

        // Inner Text Input Field
        TextField(
          value = inputText,
          onValueChange = onTextChanged,
          enabled = !isSending,
          placeholder = {
            Text(
              text = if (attachedImage != null) {
                "Que veux-tu savoir sur cette photo ?"
              } else {
                stringResource(R.string.input_placeholder)
              },
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
          },
          keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = if (isSendEnabled) ImeAction.Send else ImeAction.Default
          ),
          keyboardActions = KeyboardActions(
            onSend = {
              if (isSendEnabled) {
                onSendClicked()
              }
            }
          ),
          maxLines = 4,
          colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
          ),
          modifier = Modifier
            .weight(1f)
            .testTag("message_input")
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Send Button
        val sendGradient = Brush.linearGradient(
          colors = listOf(ElectricBlue, MysticViolet)
        )

        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .then(
              if (isSendEnabled) {
                Modifier.background(sendGradient)
              } else {
                Modifier.background(
                  if (isDark) Color(0x3316253D) else Color(0x33D6E2F2)
                )
              }
            ),
          contentAlignment = Alignment.Center
        ) {
          if (isSending) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              strokeWidth = 2.dp,
              color = Color.White
            )
          } else {
            IconButton(
              onClick = onSendClicked,
              enabled = isSendEnabled,
              modifier = Modifier
                .size(48.dp)
                .testTag("send_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(R.string.send_button_desc),
                tint = if (isSendEnabled) {
                  Color.White
                } else {
                  MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                },
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }
      }
    }
  }
}
