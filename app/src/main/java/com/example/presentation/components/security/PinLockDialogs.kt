package com.example.presentation.components.security

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.presentation.components.glass.liquidGlass
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.LuminousCyan

@Composable
fun PinLockOverlay(
  pinInput: String,
  isError: Boolean,
  onDigitClicked: (String) -> Unit,
  onBackspaceClicked: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background.copy(alpha = 0.96f))
      .padding(24.dp)
      .testTag("pin_lock_overlay"),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.fillMaxWidth(0.85f)
    ) {
      Box(
        modifier = Modifier
          .size(56.dp)
          .clip(CircleShape)
          .background(ElectricBlue),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Outlined.Lock,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(28.dp)
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "RIZZ AI VERROUILLÉ",
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Black,
          letterSpacing = 1.sp
        ),
        color = MaterialTheme.colorScheme.onSurface
      )

      Text(
        text = "Saisissez votre code PIN à 4 chiffres",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(24.dp))

      // 4 Dots
      Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        for (i in 0 until 4) {
          val isFilled = i < pinInput.length
          Box(
            modifier = Modifier
              .size(16.dp)
              .clip(CircleShape)
              .background(
                if (isError) Color(0xFFF43F5E)
                else if (isFilled) ElectricBlue
                else MaterialTheme.colorScheme.surfaceVariant
              )
          )
        }
      }

      if (isError) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "Code PIN erroné, réessayez.",
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
          color = Color(0xFFF43F5E)
        )
      }

      Spacer(modifier = Modifier.height(32.dp))

      // 1-9 Keypad
      val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "DEL")
      )

      for (row in rows) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly
        ) {
          for (item in row) {
            if (item.isEmpty()) {
              Spacer(modifier = Modifier.size(64.dp))
            } else if (item == "DEL") {
              IconButton(
                onClick = onBackspaceClicked,
                modifier = Modifier
                  .size(64.dp)
                  .testTag("pin_backspace")
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Outlined.Backspace,
                  contentDescription = "Effacer",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            } else {
              Box(
                modifier = Modifier
                  .size(64.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                  .clickable { onDigitClicked(item) }
                  .testTag("pin_key_$item"),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = item,
                  style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }
        }
        Spacer(modifier = Modifier.height(12.dp))
      }
    }
  }
}

@Composable
fun SetPinDialog(
  currentInput: String,
  onDigitClicked: (String) -> Unit,
  onBackspaceClicked: () -> Unit,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Box(
      modifier = Modifier
        .fillMaxWidth(0.9f)
        .liquidGlass(RoundedCornerShape(24.dp), isElevated = true)
        .padding(20.dp)
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Définir un code PIN",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          IconButton(onClick = onDismiss) {
            Icon(imageVector = Icons.Outlined.Close, contentDescription = "Fermer")
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "Choisissez un code à 4 chiffres :",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          for (i in 0 until 4) {
            val isFilled = i < currentInput.length
            Box(
              modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(if (isFilled) ElectricBlue else MaterialTheme.colorScheme.surfaceVariant)
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Mini keypad
        val rows = listOf(
          listOf("1", "2", "3"),
          listOf("4", "5", "6"),
          listOf("7", "8", "9"),
          listOf("", "0", "DEL")
        )

        for (row in rows) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
          ) {
            for (item in row) {
              if (item.isEmpty()) {
                Spacer(modifier = Modifier.size(52.dp))
              } else if (item == "DEL") {
                IconButton(onClick = onBackspaceClicked, modifier = Modifier.size(52.dp)) {
                  Icon(imageVector = Icons.AutoMirrored.Outlined.Backspace, contentDescription = "Effacer")
                }
              } else {
                Box(
                  modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable { onDigitClicked(item) },
                  contentAlignment = Alignment.Center
                ) {
                  Text(text = item, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
          Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
          onClick = onConfirm,
          enabled = currentInput.length == 4,
          colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Valider ce code PIN", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
