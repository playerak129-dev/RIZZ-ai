package com.example.presentation.components.style

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.domain.model.ConversationStyle
import com.example.domain.model.StyleSettings
import com.example.presentation.components.glass.liquidGlass
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.LuminousCyan
import com.example.ui.theme.MysticViolet

@Composable
fun StyleBlendDialog(
  styleSettings: StyleSettings,
  onToggleBlendMode: (Boolean) -> Unit,
  onWeightChanged: (ConversationStyle, Int) -> Unit,
  onResetWeights: () -> Unit,
  onDismiss: () -> Unit
) {
  val normalized = styleSettings.getNormalizedWeights()
  val blendableStyles = listOf(
    ConversationStyle.CHARO,
    ConversationStyle.HUMOUR,
    ConversationStyle.ROMANTIQUE,
    ConversationStyle.CALCULATEUR
  )

  Dialog(onDismissRequest = onDismiss) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .liquidGlass(
          shape = RoundedCornerShape(28.dp),
          isElevated = true,
          borderWidth = 1.5.dp
        )
        .padding(22.dp)
        .testTag("style_blend_dialog")
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        // Title & Switch
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "Mélange de Styles",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Pondération totale : 100%",
              style = MaterialTheme.typography.bodySmall,
              color = LuminousCyan
            )
          }

          Switch(
            checked = styleSettings.isBlendModeEnabled,
            onCheckedChange = onToggleBlendMode,
            colors = SwitchDefaults.colors(
              checkedThumbColor = LuminousCyan,
              checkedTrackColor = ElectricBlue.copy(alpha = 0.5f)
            ),
            modifier = Modifier.testTag("blend_mode_switch")
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sliders for each style
        blendableStyles.forEach { style ->
          val weight = normalized[style] ?: 0

          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = style.emoji, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = style.displayName,
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
              Text(
                text = "$weight%",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = if (styleSettings.isBlendModeEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Slider(
              value = weight.toFloat(),
              onValueChange = { newVal ->
                if (styleSettings.isBlendModeEnabled) {
                  onWeightChanged(style, newVal.toInt())
                }
              },
              valueRange = 0f..100f,
              enabled = styleSettings.isBlendModeEnabled,
              colors = SliderDefaults.colors(
                thumbColor = ElectricBlue,
                activeTrackColor = MysticViolet,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
              ),
              modifier = Modifier.testTag("slider_${style.name}")
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom action buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedButton(
            onClick = onResetWeights,
            enabled = styleSettings.isBlendModeEnabled,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.testTag("reset_weights_button")
          ) {
            Icon(
              imageVector = Icons.Outlined.RestartAlt,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "Réinitialiser", style = MaterialTheme.typography.labelSmall)
          }

          Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
            modifier = Modifier.testTag("close_blend_dialog_button")
          ) {
            Text(text = "Appliquer", color = androidx.compose.ui.graphics.Color.White)
          }
        }
      }
    }
  }
}
