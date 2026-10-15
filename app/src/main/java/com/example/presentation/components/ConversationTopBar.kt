package com.example.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.presentation.components.glass.liquidGlass
import com.example.presentation.components.orb.OrbState
import com.example.presentation.components.orb.RizzAiOrb
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.LuminousCyan
import com.example.ui.theme.MysticViolet
import com.example.ui.theme.ThemeMode

@Composable
fun ConversationTopBar(
  messageCount: Int,
  themeMode: ThemeMode,
  orbState: OrbState,
  onOpenHistoryClicked: () -> Unit,
  onOpenStrategicBoardClicked: () -> Unit,
  onOpenRizzRapideClicked: () -> Unit,
  onOpenSettingsClicked: () -> Unit,
  onThemeToggleClicked: () -> Unit,
  onClearClicked: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .statusBarsPadding()
      .padding(horizontal = 16.dp, vertical = 8.dp)
      .liquidGlass(
        shape = RoundedCornerShape(22.dp),
        isElevated = true,
        borderWidth = 1.dp
      )
      .padding(horizontal = 14.dp, vertical = 8.dp)
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // App identity with live mini-orb avatar
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape),
            contentAlignment = Alignment.Center
          ) {
            RizzAiOrb(
              state = orbState,
              diameter = 36.dp
            )
          }

          Spacer(modifier = Modifier.width(10.dp))

          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "RIZZ AI",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Black,
                  letterSpacing = 0.8.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
              )
              Spacer(modifier = Modifier.width(4.dp))
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(MaterialTheme.colorScheme.primaryContainer)
                  .padding(horizontal = 5.dp, vertical = 1.dp)
              ) {
                Text(
                  text = "2.0",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                  ),
                  color = MaterialTheme.colorScheme.onPrimaryContainer
                )
              }
            }

            Text(
              text = stringResource(R.string.app_edition),
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                fontSize = 9.sp
              ),
              color = MaterialTheme.colorScheme.primary
            )
          }
        }

        // Action controls
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
          // History button
          IconButton(
            onClick = onOpenHistoryClicked,
            modifier = Modifier
              .size(40.dp)
              .testTag("open_history_button")
          ) {
            Icon(
              imageVector = Icons.Outlined.History,
              contentDescription = "Historique local",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(22.dp)
            )
          }

          // Strategic Chessboard shortcut button
          IconButton(
            onClick = onOpenStrategicBoardClicked,
            modifier = Modifier
              .size(40.dp)
              .testTag("open_strategic_board_button")
          ) {
            Icon(
              imageVector = Icons.Outlined.Explore,
              contentDescription = "Échiquier stratégique",
              tint = MysticViolet,
              modifier = Modifier.size(22.dp)
            )
          }

          // Rizz Rapide shortcut button
          IconButton(
            onClick = onOpenRizzRapideClicked,
            modifier = Modifier
              .size(40.dp)
              .testTag("open_rizz_rapide_button")
          ) {
            Icon(
              imageVector = Icons.Outlined.FlashOn,
              contentDescription = stringResource(R.string.open_rizz_rapide_desc),
              tint = LuminousCyan,
              modifier = Modifier.size(22.dp)
            )
          }

          // Settings shortcut button
          IconButton(
            onClick = onOpenSettingsClicked,
            modifier = Modifier
              .size(40.dp)
              .testTag("open_settings_button")
          ) {
            Icon(
              imageVector = Icons.Outlined.Settings,
              contentDescription = "Paramètres & Confidentialité",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(22.dp)
            )
          }

          // Theme mode toggle button
          IconButton(
            onClick = onThemeToggleClicked,
            modifier = Modifier
              .size(40.dp)
              .testTag("theme_toggle_button")
          ) {
            val themeIcon = when (themeMode) {
              ThemeMode.SYSTEM -> Icons.Outlined.BrightnessAuto
              ThemeMode.DARK -> Icons.Outlined.DarkMode
              ThemeMode.LIGHT -> Icons.Outlined.LightMode
            }
            Icon(
              imageVector = themeIcon,
              contentDescription = "Basculer le thème",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp)
            )
          }

          if (messageCount > 0) {
            IconButton(
              onClick = onClearClicked,
              modifier = Modifier
                .size(40.dp)
                .testTag("clear_button")
            ) {
              Icon(
                imageVector = Icons.Outlined.DeleteSweep,
                contentDescription = stringResource(R.string.clear_history),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }
      }

      // Status Sub-bar
      Row(
        modifier = Modifier
          .padding(top = 4.dp)
          .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = "Échiquier • Rizz Rapide • Room Persistant",
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
        )

        if (messageCount > 0) {
          Text(
            text = "$messageCount message(s)",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, fontSize = 11.sp),
            color = MaterialTheme.colorScheme.primary
          )
        }
      }
    }
  }
}
