package com.example.presentation.components.history

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.core.DateTimeUtils
import com.example.domain.model.ConversationSummary
import com.example.presentation.components.glass.liquidGlass
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.LuminousCyan

@Composable
fun ConversationHistorySheet(
  conversationList: List<ConversationSummary>,
  searchQuery: String,
  currentConversationId: String?,
  onSearchQueryChanged: (String) -> Unit,
  onNewConversationClicked: () -> Unit,
  onSelectConversation: (String) -> Unit,
  onRenameClicked: (String, String) -> Unit,
  onDeleteClicked: (String) -> Unit,
  onOpenExportClicked: () -> Unit,
  onOpenImportClicked: () -> Unit,
  onDismiss: () -> Unit
) {
  val filteredList = if (searchQuery.isBlank()) {
    conversationList
  } else {
    conversationList.filter {
      it.title.contains(searchQuery, ignoreCase = true) ||
          it.lastMessageSnippet.contains(searchQuery, ignoreCase = true)
    }
  }

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
        .testTag("history_sheet_dialog")
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
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
                imageVector = Icons.Outlined.History,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "HISTORIQUE LOCAL",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Black,
                  letterSpacing = 0.8.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "${conversationList.size} discussion(s) enregistrée(s)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }

          IconButton(onClick = onDismiss) {
            Icon(
              imageVector = Icons.Outlined.Close,
              contentDescription = "Fermer l'historique",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Actions Bar (+ Nouvelle discussion, Export, Import)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Button(
            onClick = onNewConversationClicked,
            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
            modifier = Modifier
              .weight(1f)
              .testTag("new_conversation_button")
          ) {
            Icon(imageVector = Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Nouvelle", fontWeight = FontWeight.Bold)
          }

          IconButton(
            onClick = onOpenExportClicked,
            modifier = Modifier.size(42.dp)
          ) {
            Icon(
              imageVector = Icons.Outlined.FileDownload,
              contentDescription = "Exporter JSON",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          IconButton(
            onClick = onOpenImportClicked,
            modifier = Modifier.size(42.dp)
          ) {
            Icon(
              imageVector = Icons.Outlined.FileUpload,
              contentDescription = "Importer JSON",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
          value = searchQuery,
          onValueChange = onSearchQueryChanged,
          placeholder = { Text("Rechercher dans les discussions...") },
          leadingIcon = {
            Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
          },
          singleLine = true,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ElectricBlue,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("search_history_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Conversations List
        if (filteredList.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(180.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = if (searchQuery.isNotBlank()) "Aucune discussion ne correspond à votre recherche." else "Aucune discussion pour l'instant.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .height(340.dp)
          ) {
            items(filteredList, key = { it.id }) { conv ->
              val isSelected = conv.id == currentConversationId
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp)
                  .clip(RoundedCornerShape(16.dp))
                  .background(
                    if (isSelected) ElectricBlue.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                  )
                  .clickable { onSelectConversation(conv.id) }
                  .padding(12.dp)
                  .testTag("conversation_item_${conv.id}")
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(
                        text = conv.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = "• ${conv.messageCount} msg",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                      )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                      text = conv.lastMessageSnippet,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                      text = DateTimeUtils.formatTime(conv.updatedAt),
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                  }

                  Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                      onClick = { onRenameClicked(conv.id, conv.title) },
                      modifier = Modifier.size(36.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Renommer",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                      )
                    }

                    IconButton(
                      onClick = { onDeleteClicked(conv.id) },
                      modifier = Modifier.size(36.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Supprimer",
                        tint = Color(0xFFF43F5E),
                        modifier = Modifier.size(16.dp)
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
