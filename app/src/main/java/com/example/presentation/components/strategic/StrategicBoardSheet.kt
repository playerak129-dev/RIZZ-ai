package com.example.presentation.components.strategic

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import com.example.core.MarkdownFormatter
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
import com.example.domain.model.strategic.ComparisonResult
import com.example.domain.model.strategic.StrategicBoardResult
import com.example.domain.model.strategic.StrategicOption
import com.example.presentation.components.glass.liquidGlass
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.LuminousCyan
import com.example.ui.theme.MysticViolet

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StrategicBoardSheet(
  situationText: String,
  boardResult: StrategicBoardResult?,
  isGenerating: Boolean,
  errorMessage: String?,
  isCoachModeActive: Boolean,
  selectedOptionId: String?,
  realReplyInput: String,
  isAnalyzingRealReply: Boolean,
  comparisonResult: ComparisonResult?,
  onSituationChanged: (String) -> Unit,
  onGenerateClicked: () -> Unit,
  onToggleCoachMode: (Boolean) -> Unit,
  onSelectOption: (String) -> Unit,
  onRealReplyChanged: (String) -> Unit,
  onAnalyzeRealReply: () -> Unit,
  onUseReply: (String) -> Unit,
  onSendReplyDirectly: (String) -> Unit,
  onCopiedToast: (String) -> Unit,
  onDismiss: () -> Unit
) {
  val clipboardManager = LocalClipboardManager.current
  val selectedOption: StrategicOption? = boardResult?.options?.firstOrNull { it.id == selectedOptionId }
    ?: boardResult?.options?.firstOrNull()

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
        .testTag("strategic_board_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
      ) {
        // Top Header
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
                .background(Brush.linearGradient(listOf(MysticViolet, ElectricBlue))),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Outlined.Explore,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "ÉCHIQUIER STRATÉGIQUE",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Black,
                  letterSpacing = 0.8.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Anticipation des branches & Mode Coach",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }

          IconButton(onClick = onDismiss) {
            Icon(
              imageVector = Icons.Outlined.Close,
              contentDescription = "Fermer l'échiquier",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Mode Switcher Tabs (Échiquier vs Coach)
        TabRow(
          selectedTabIndex = if (isCoachModeActive) 1 else 0,
          containerColor = Color.Transparent,
          contentColor = MaterialTheme.colorScheme.primary,
          indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
              modifier = Modifier.tabIndicatorOffset(tabPositions[if (isCoachModeActive) 1 else 0]),
              color = ElectricBlue
            )
          }
        ) {
          Tab(
            selected = !isCoachModeActive,
            onClick = { onToggleCoachMode(false) },
            text = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Outlined.Explore, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Branches & Coups", fontWeight = FontWeight.Bold)
              }
            }
          )
          Tab(
            selected = isCoachModeActive,
            onClick = { onToggleCoachMode(true) },
            text = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Outlined.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Mode Coach IA", fontWeight = FontWeight.Bold)
              }
            }
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Situation Input
        OutlinedTextField(
          value = situationText,
          onValueChange = onSituationChanged,
          label = { Text("Message reçu ou situation à analyser") },
          placeholder = { Text("Ex: 'Elle dit : Je ne sais pas trop, tu proposes quoi ?'") },
          maxLines = 3,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ElectricBlue,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("strategic_situation_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Analyze / Generate Button
        Button(
          onClick = onGenerateClicked,
          enabled = situationText.isNotBlank() && !isGenerating,
          colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("generate_strategic_board_button")
        ) {
          if (isGenerating) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              strokeWidth = 2.dp,
              color = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Calcul des branches stratégiques...", color = Color.White)
          } else {
            Icon(
              imageVector = Icons.Outlined.Explore,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
              tint = Color.White
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (boardResult != null) "Recalculer les branches" else "Déployer l'échiquier",
              color = Color.White,
              fontWeight = FontWeight.Bold
            )
          }
        }

        if (errorMessage != null) {
          Spacer(modifier = Modifier.height(10.dp))
          Text(text = errorMessage, color = Color(0xFFF43F5E), style = MaterialTheme.typography.bodySmall)
        }

        // --- CONTENT AREA ---
        if (boardResult != null) {
          Spacer(modifier = Modifier.height(14.dp))

          // Mandatory Disclaimer
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
              .padding(10.dp)
          ) {
            Row(verticalAlignment = Alignment.Top) {
              Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                  .size(18.dp)
                  .padding(top = 1.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = boardResult.disclaimer,
                style = MaterialTheme.typography.labelSmall.copy(lineHeight = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          if (isCoachModeActive) {
            // === COACH MODE ===
            Spacer(modifier = Modifier.height(14.dp))
            Text(
              text = "Analyse psychologique du Coach",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = MysticViolet
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(RoundedCornerShape(16.dp), isElevated = false)
                .padding(14.dp)
            ) {
              Text(
                text = boardResult.coachAdvice,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "Dynamique de l'échange",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = LuminousCyan
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(RoundedCornerShape(16.dp), isElevated = false)
                .padding(14.dp)
            ) {
              Text(
                text = boardResult.analysis,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          } else {
            // === STRATEGIC CHESSBOARD ===
            Spacer(modifier = Modifier.height(14.dp))

            // Option selection chips
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              boardResult.options.forEach { opt ->
                val isSelected = opt.id == selectedOption?.id
                FilterChip(
                  selected = isSelected,
                  onClick = { onSelectOption(opt.id) },
                  label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      if (opt.isRecommended) {
                        Icon(
                          imageVector = Icons.Outlined.Star,
                          contentDescription = "Option recommandée",
                          tint = Color(0xFFFFB703),
                          modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                      }
                      Text("${opt.name} (${opt.score}/100)")
                    }
                  },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ElectricBlue.copy(alpha = 0.25f),
                    selectedLabelColor = ElectricBlue
                  )
                )
              }
            }

            if (selectedOption != null) {
              Spacer(modifier = Modifier.height(12.dp))

              // Selected Option Card
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .liquidGlass(
                    shape = RoundedCornerShape(18.dp),
                    isElevated = true,
                    borderWidth = 1.dp
                  )
                  .padding(14.dp)
              ) {
                Column {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(
                        text = selectedOption.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                      )
                      if (selectedOption.isRecommended) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                          modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFFB703).copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                          Text(
                            text = "★ Recommandé",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFFB703)
                          )
                        }
                      }
                    }

                    Text(
                      text = "Ton : ${selectedOption.tone}",
                      style = MaterialTheme.typography.labelSmall,
                      color = LuminousCyan
                    )
                  }

                  Spacer(modifier = Modifier.height(10.dp))

                  // Proposed Reply Text
                  val formattedReply = remember(selectedOption.proposedReply) {
                    MarkdownFormatter.formatToAnnotatedString(
                      input = selectedOption.proposedReply,
                      isDark = true
                    )
                  }
                  Text(
                    text = formattedReply,
                    style = MaterialTheme.typography.bodyLarge.copy(
                      fontWeight = FontWeight.SemiBold,
                      lineHeight = 24.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                  )

                  Spacer(modifier = Modifier.height(12.dp))

                  // Action Buttons for this reply
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    Button(
                      onClick = {
                        clipboardManager.setText(AnnotatedString(selectedOption.proposedReply))
                        onCopiedToast("Réponse copiée !")
                      },
                      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                      modifier = Modifier.weight(1f)
                    ) {
                      Icon(imageVector = Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Copier", style = MaterialTheme.typography.labelSmall)
                    }

                    Button(
                      onClick = { onUseReply(selectedOption.proposedReply) },
                      colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                      modifier = Modifier.weight(1.2f)
                    ) {
                      Icon(imageVector = Icons.AutoMirrored.Outlined.Input, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Dans le chat", style = MaterialTheme.typography.labelSmall)
                    }

                    Button(
                      onClick = { onSendReplyDirectly(selectedOption.proposedReply) },
                      colors = ButtonDefaults.buttonColors(containerColor = MysticViolet),
                      modifier = Modifier.weight(1.2f)
                    ) {
                      Icon(imageVector = Icons.AutoMirrored.Outlined.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("Envoyer direct", style = MaterialTheme.typography.labelSmall)
                    }
                  }

                  Spacer(modifier = Modifier.height(12.dp))

                  // Strengths & Risks
                  Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = "Forces :",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF10B981)
                      )
                      selectedOption.strengths.forEach { s ->
                        Text("• $s", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                      }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = "Risques :",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFF43F5E)
                      )
                      selectedOption.risks.forEach { r ->
                        Text("• $r", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                      }
                    }
                  }

                  // Anticipated Branches & Counter-replies
                  if (selectedOption.branches.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                      text = "Branches d'anticipation (Coups suivants) :",
                      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    selectedOption.branches.forEachIndexed { bIndex, branch ->
                      Box(
                        modifier = Modifier
                          .fillMaxWidth()
                          .padding(vertical = 4.dp)
                          .clip(RoundedCornerShape(12.dp))
                          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                          .padding(10.dp)
                      ) {
                        Column {
                          Text(
                            text = "Condition : ${branch.trigger}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = LuminousCyan
                          )
                          Text(
                            text = "Hypothèse : ${branch.hypothesis}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                          )
                          Spacer(modifier = Modifier.height(4.dp))
                          Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                          ) {
                            Text(
                              text = "↳ Contre-réponse : \"${branch.counterReply}\"",
                              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                              color = MaterialTheme.colorScheme.onSurface,
                              modifier = Modifier.weight(1f)
                            )
                            IconButton(
                              onClick = {
                                clipboardManager.setText(AnnotatedString(branch.counterReply))
                                onCopiedToast("Contre-réponse copiée !")
                              },
                              modifier = Modifier.size(32.dp)
                            ) {
                              Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Copier",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
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

            // === REAL RECEIVED REPLY COMPARISON SECTION ===
            Spacer(modifier = Modifier.height(18.dp))
            Text(
              text = "Confronter la réalité aux prévisions",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Saisis la réponse que tu as réellement reçue pour adapter la stratégie.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
              value = realReplyInput,
              onValueChange = onRealReplyChanged,
              placeholder = { Text("Ex: Elle a répondu : 'mdrrr t'es trop bête'") },
              modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
              onClick = onAnalyzeRealReply,
              enabled = realReplyInput.isNotBlank() && !isAnalyzingRealReply,
              colors = ButtonDefaults.buttonColors(containerColor = MysticViolet),
              modifier = Modifier.fillMaxWidth()
            ) {
              if (isAnalyzingRealReply) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Analyse de l'écart...")
              } else {
                Text("Comparer avec les prévisions", fontWeight = FontWeight.Bold)
              }
            }

            // Comparison Feedback Result
            if (comparisonResult != null) {
              Spacer(modifier = Modifier.height(10.dp))
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .liquidGlass(RoundedCornerShape(16.dp), isElevated = false)
                  .padding(12.dp)
              ) {
                Column {
                  Text(
                    text = "Branche identifiée : ${comparisonResult.matchedBranch ?: "Réaction imprévue"}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = LuminousCyan
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = comparisonResult.interpretation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = "Coup suivant conseillé :",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = ElectricBlue
                  )
                  Text(
                    text = "\"${comparisonResult.recommendedNextMove}\"",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Spacer(modifier = Modifier.height(6.dp))
                  Text(
                    text = "Conseil Coach : ${comparisonResult.coachInsight}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
