package com.example.presentation.components.backup

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.presentation.components.glass.liquidGlass
import com.example.ui.theme.ElectricBlue

@Composable
fun ExportDialog(
  jsonData: String,
  onDismiss: () -> Unit,
  onCopiedToast: () -> Unit
) {
  val clipboardManager = LocalClipboardManager.current

  Dialog(onDismissRequest = onDismiss) {
    Box(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .liquidGlass(RoundedCornerShape(24.dp), isElevated = true)
        .padding(20.dp)
        .testTag("export_dialog")
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Outlined.FileDownload, contentDescription = null, tint = ElectricBlue)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Exporter au format JSON", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }
          IconButton(onClick = onDismiss) {
            Icon(imageVector = Icons.Outlined.Close, contentDescription = "Fermer")
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = "Vos conversations sont encodées selon un format JSON standard avec horodatages et identifiants uniques.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(10.dp)
            .verticalScroll(rememberScrollState())
        ) {
          Text(
            text = jsonData,
            style = MaterialTheme.typography.bodySmall.copy(
              fontFamily = FontFamily.Monospace,
              fontSize = 11.sp,
              lineHeight = 15.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = {
            clipboardManager.setText(AnnotatedString(jsonData))
            onCopiedToast()
          },
          colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Copier le JSON dans le presse-papier", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
fun ImportDialog(
  inputJson: String,
  errorMessage: String?,
  onInputChanged: (String) -> Unit,
  onConfirmImport: () -> Unit,
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Box(
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .liquidGlass(RoundedCornerShape(24.dp), isElevated = true)
        .padding(20.dp)
        .testTag("import_dialog")
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Outlined.FileUpload, contentDescription = null, tint = ElectricBlue)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Importer depuis un JSON", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }
          IconButton(onClick = onDismiss) {
            Icon(imageVector = Icons.Outlined.Close, contentDescription = "Fermer")
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = "Collez ici les données exportées d'une session RIZZ AI. Les discussions et messages seront validés et importés localement.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = inputJson,
          onValueChange = onInputChanged,
          placeholder = { Text("Coller le JSON ici...") },
          modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
          colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricBlue)
        )

        if (errorMessage != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(text = errorMessage, color = Color(0xFFF43F5E), style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = onConfirmImport,
          enabled = inputJson.isNotBlank(),
          colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Valider et importer", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
fun RenameDialog(
  currentTitle: String,
  onTitleChanged: (String) -> Unit,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Box(
      modifier = Modifier
        .fillMaxWidth(0.9f)
        .liquidGlass(RoundedCornerShape(20.dp), isElevated = true)
        .padding(18.dp)
        .testTag("rename_dialog")
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text("Renommer la discussion", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = currentTitle,
          onValueChange = onTitleChanged,
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricBlue)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(onClick = onDismiss) {
            Text("Annuler")
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = onConfirm,
            enabled = currentTitle.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
          ) {
            Text("Enregistrer")
          }
        }
      }
    }
  }
}
