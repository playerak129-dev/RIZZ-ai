package com.example.presentation.components.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.ui.theme.MysticViolet
import com.example.ui.theme.ThemeMode

@Composable
fun SettingsDialog(
  themeMode: ThemeMode,
  language: String,
  isPinLockEnabled: Boolean,
  onThemeModeSelected: (ThemeMode) -> Unit,
  onLanguageSelected: (String) -> Unit,
  onOpenSetPinClicked: () -> Unit,
  onDisablePinClicked: () -> Unit,
  onDeleteAllDataClicked: () -> Unit,
  onDismiss: () -> Unit
) {
  var showDeleteConfirmation by remember { mutableStateOf(false) }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth(0.94f)
        .liquidGlass(
          shape = RoundedCornerShape(26.dp),
          isElevated = true,
          borderWidth = 1.5.dp
        )
        .padding(20.dp)
        .testTag("settings_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(ElectricBlue),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "PARAMÈTRES & SÉCURITÉ",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Black,
                  letterSpacing = 0.8.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Confidentialité, PIN & Thèmes",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }

          IconButton(onClick = onDismiss) {
            Icon(
              imageVector = Icons.Outlined.Close,
              contentDescription = "Fermer les paramètres",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Theme Selector
        Text(
          text = "Apparence & Thème",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          ThemeOptionChip(
            title = "Auto",
            icon = Icons.Outlined.BrightnessAuto,
            isSelected = themeMode == ThemeMode.SYSTEM,
            onClick = { onThemeModeSelected(ThemeMode.SYSTEM) },
            modifier = Modifier.weight(1f)
          )
          ThemeOptionChip(
            title = "Sombre",
            icon = Icons.Outlined.DarkMode,
            isSelected = themeMode == ThemeMode.DARK,
            onClick = { onThemeModeSelected(ThemeMode.DARK) },
            modifier = Modifier.weight(1f)
          )
          ThemeOptionChip(
            title = "Clair",
            icon = Icons.Outlined.LightMode,
            isSelected = themeMode == ThemeMode.LIGHT,
            onClick = { onThemeModeSelected(ThemeMode.LIGHT) },
            modifier = Modifier.weight(1f)
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Language Selector
        Text(
          text = "Langue de l'interface",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          LanguageOptionChip(
            title = "Français (FR)",
            isSelected = language == "fr",
            onClick = { onLanguageSelected("fr") },
            modifier = Modifier.weight(1f)
          )
          LanguageOptionChip(
            title = "English (EN)",
            isSelected = language == "en",
            onClick = { onLanguageSelected("en") },
            modifier = Modifier.weight(1f)
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Local PIN Lock
        Text(
          text = "Verrouillage local par code PIN",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(RoundedCornerShape(16.dp), isElevated = false)
            .padding(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = if (isPinLockEnabled) "Verrouillage actif" else "Verrouillage désactivé",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Protège vos conversations avec un code à 4 chiffres.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Switch(
              checked = isPinLockEnabled,
              onCheckedChange = { enabled ->
                if (enabled) onOpenSetPinClicked() else onDisablePinClicked()
              },
              colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ElectricBlue)
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. AI Transparency Notice & Security
        Text(
          text = "Transparence IA & Confidentialité",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(12.dp)
        ) {
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(imageVector = Icons.Outlined.Shield, contentDescription = null, tint = LuminousCyan, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Données transmises au service d'IA", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Les messages et photos sont transmis de manière éphémère aux serveurs Google Gemini pour générer les réponses. Aucune donnée n'est revendue. L'historique des discussions est conservé à 100% localement sur votre téléphone via Room.",
              style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(imageVector = Icons.Outlined.Key, contentDescription = null, tint = MysticViolet, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Architecture de clé API & Sécurité", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Note de sécurité : toute clé intégrée dans un APK Android peut théoriquement être extraite par rétro-ingénierie. Pour un déploiement public en production, prévoyez une architecture avec proxy serveur sécurisé ou Firebase App Check.",
              style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Danger Zone (Delete All Data)
        Text(
          text = "Zone de danger",
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = Color(0xFFF43F5E)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Button(
          onClick = { showDeleteConfirmation = true },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E).copy(alpha = 0.15f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(imageVector = Icons.Outlined.DeleteForever, contentDescription = null, tint = Color(0xFFF43F5E), modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Supprimer toutes les données locales", color = Color(0xFFF43F5E), fontWeight = FontWeight.Bold)
        }
      }
    }
  }

  // Delete Confirmation Alert Dialog
  if (showDeleteConfirmation) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirmation = false },
      title = { Text("Supprimer toutes les données ?") },
      text = { Text("Cette action supprimera définitivement toutes les conversations, messages et préférences enregistrés sur cet appareil. Cette action est irréversible.") },
      confirmButton = {
        Button(
          onClick = {
            showDeleteConfirmation = false
            onDeleteAllDataClicked()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E))
        ) {
          Text("Oui, tout supprimer", color = Color.White)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteConfirmation = false }) {
          Text("Annuler")
        }
      }
    )
  }
}

@Composable
private fun ThemeOptionChip(
  title: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .background(
        if (isSelected) ElectricBlue.copy(alpha = 0.25f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
      )
      .clickable(onClick = onClick)
      .padding(vertical = 10.dp, horizontal = 8.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (isSelected) ElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp)
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
        color = if (isSelected) ElectricBlue else MaterialTheme.colorScheme.onSurface
      )
    }
  }
}

@Composable
private fun LanguageOptionChip(
  title: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .background(
        if (isSelected) ElectricBlue.copy(alpha = 0.25f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
      )
      .clickable(onClick = onClick)
      .padding(vertical = 10.dp, horizontal = 12.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
      color = if (isSelected) ElectricBlue else MaterialTheme.colorScheme.onSurface
    )
  }
}
