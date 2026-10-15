package com.example.presentation.components.rizzrapide

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.outlined.Input
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.model.StyleSettings
import com.example.presentation.components.glass.liquidGlass
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.LuminousCyan
import com.example.ui.theme.MysticViolet

import androidx.compose.material.icons.automirrored.outlined.Send

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RizzRapideSheet(
  situationText: String,
  options: List<String>,
  isGenerating: Boolean,
  errorMessage: String?,
  styleSettings: StyleSettings,
  onSituationChanged: (String) -> Unit,
  onGenerateClicked: () -> Unit,
  onOptionSelected: (String) -> Unit,
  onSendDirectlyClicked: ((String) -> Unit)? = null,
  onCopiedToast: (String) -> Unit,
  onDismiss: () -> Unit
) {
  val clipboardManager = LocalClipboardManager.current
  val quickStarters = listOf(
    "Elle me met un vu",
    "Tu fais quoi de beau ?",
    "Relance après 2 jours"
  )

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .liquidGlass(
          shape = RoundedCornerShape(26.dp),
          isElevated = true,
          borderWidth = 1.5.dp
        )
        .padding(20.dp)
        .testTag("rizz_rapide_dialog")
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
                .background(Brush.linearGradient(listOf(ElectricBlue, LuminousCyan))),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Outlined.FlashOn,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "RIZZ RAPIDE",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Black,
                  letterSpacing = 0.8.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "3 réparties immédiates • ${styleSettings.activeStyle.displayName} (Niv. ${styleSettings.intensity})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }

          IconButton(onClick = onDismiss) {
            Icon(
              imageVector = Icons.Outlined.Close,
              contentDescription = "Fermer",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Situation Input
        OutlinedTextField(
          value = situationText,
          onValueChange = onSituationChanged,
          placeholder = {
            Text(
              text = "Ex: 'Elle me demande si je viens ce soir', 'Vu depuis hier'...",
              style = MaterialTheme.typography.bodySmall
            )
          },
          maxLines = 3,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ElectricBlue,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("rizz_rapide_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Starter Chips
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          quickStarters.forEach { starter ->
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .clickable { onSituationChanged(starter) }
                .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
              Text(
                text = starter,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Generate / Regenerate Button
        Button(
          onClick = onGenerateClicked,
          enabled = situationText.isNotBlank() && !isGenerating,
          colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("generate_rizz_rapide_button")
        ) {
          if (isGenerating) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              strokeWidth = 2.dp,
              color = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Génération des 3 options...", color = Color.White)
          } else {
            Icon(
              imageVector = Icons.Outlined.FlashOn,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
              tint = Color.White
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (options.isNotEmpty()) "Nouvelle série de réparties" else "Générer 3 réparties",
              color = Color.White,
              fontWeight = FontWeight.Bold
            )
          }
        }

        // Error message if any
        if (errorMessage != null) {
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFF43F5E)
          )
        }

        // Display Generated Options
        if (options.isNotEmpty()) {
          Spacer(modifier = Modifier.height(16.dp))
          Text(
            text = "Propositions générées :",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(8.dp))

          options.forEachIndexed { index, option ->
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .liquidGlass(
                  shape = RoundedCornerShape(16.dp),
                  isElevated = false,
                  borderWidth = 1.dp
                )
                .padding(14.dp)
                .testTag("rizz_option_$index")
            ) {
              Column {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = "Option ${index + 1}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = LuminousCyan
                  )

                  Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                      onClick = {
                        clipboardManager.setText(AnnotatedString(option))
                        onCopiedToast("Option ${index + 1} copiée !")
                      },
                      modifier = Modifier
                        .size(32.dp)
                        .testTag("copy_option_$index")
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "Copier l'option",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                      )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                      onClick = { onOptionSelected(option) },
                      modifier = Modifier
                        .size(32.dp)
                        .testTag("use_option_$index")
                    ) {
                      Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Input,
                        contentDescription = "Insérer dans la discussion",
                        tint = ElectricBlue,
                        modifier = Modifier.size(16.dp)
                      )
                    }

                    if (onSendDirectlyClicked != null) {
                      Spacer(modifier = Modifier.width(4.dp))
                      IconButton(
                        onClick = { onSendDirectlyClicked(option) },
                        modifier = Modifier
                          .size(32.dp)
                          .testTag("send_option_$index")
                      ) {
                        Icon(
                          imageVector = Icons.AutoMirrored.Outlined.Send,
                          contentDescription = "Envoyer immédiatement",
                          tint = MysticViolet,
                          modifier = Modifier.size(16.dp)
                        )
                      }
                    }
                  }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                  text = option,
                  style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }
        }
      }
    }
  }
}
