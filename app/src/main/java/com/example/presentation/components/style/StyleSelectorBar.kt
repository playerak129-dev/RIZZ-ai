package com.example.presentation.components.style

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ConversationStyle
import com.example.domain.model.StyleSettings
import com.example.presentation.components.glass.liquidGlass
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.LuminousCyan
import com.example.ui.theme.MysticViolet

@Composable
fun StyleSelectorBar(
  styleSettings: StyleSettings,
  onStyleSelected: (ConversationStyle) -> Unit,
  onIntensityChanged: (Int) -> Unit,
  onOpenBlendDialog: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Horizontal scrollable style pills
    Row(
      modifier = Modifier
        .weight(1f)
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      ConversationStyle.entries.forEach { style ->
        val isSelected = !styleSettings.isBlendModeEnabled && styleSettings.activeStyle == style

        Box(
          modifier = Modifier
            .liquidGlass(
              shape = RoundedCornerShape(16.dp),
              isElevated = isSelected,
              borderWidth = if (isSelected) 1.5.dp else 1.dp
            )
            .then(
              if (isSelected) {
                Modifier.background(
                  Brush.horizontalGradient(
                    listOf(
                      ElectricBlue.copy(alpha = 0.35f),
                      MysticViolet.copy(alpha = 0.25f)
                    )
                  )
                )
              } else Modifier
            )
            .clickable { onStyleSelected(style) }
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("style_chip_${style.name}")
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = style.emoji, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = style.displayName,
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
              ),
              color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.width(8.dp))

    // Intensity pill (Click to cycle 1 -> 2 -> 3 -> 4 -> 5 -> 1)
    Box(
      modifier = Modifier
        .liquidGlass(
          shape = RoundedCornerShape(16.dp),
          isElevated = false,
          borderWidth = 1.dp
        )
        .clickable {
          val next = if (styleSettings.intensity >= 5) 1 else styleSettings.intensity + 1
          onIntensityChanged(next)
        }
        .padding(horizontal = 10.dp, vertical = 6.dp)
        .testTag("intensity_chip")
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = "Niv. ${styleSettings.intensity}",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.secondary
        )
      }
    }

    Spacer(modifier = Modifier.width(6.dp))

    // Mix / Blend Mode Dialog Launcher
    IconButton(
      onClick = onOpenBlendDialog,
      modifier = Modifier
        .size(36.dp)
        .liquidGlass(
          shape = CircleShape,
          isElevated = styleSettings.isBlendModeEnabled,
          borderWidth = 1.dp
        )
        .testTag("blend_settings_button")
    ) {
      Icon(
        imageVector = Icons.Outlined.Tune,
        contentDescription = "Réglages de mélange de styles",
        tint = if (styleSettings.isBlendModeEnabled) LuminousCyan else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(18.dp)
      )
    }
  }
}
