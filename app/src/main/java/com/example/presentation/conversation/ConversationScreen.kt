package com.example.presentation.conversation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.domain.model.MessageRole
import com.example.presentation.components.ConversationEmptyState
import com.example.presentation.components.ConversationInputField
import com.example.presentation.components.ConversationTopBar
import com.example.presentation.components.MessageBubble
import com.example.presentation.components.backup.ExportDialog
import com.example.presentation.components.backup.ImportDialog
import com.example.presentation.components.backup.RenameDialog
import com.example.presentation.components.glass.liquidGlass
import com.example.presentation.components.history.ConversationHistorySheet
import com.example.presentation.components.rizzrapide.RizzRapideSheet
import com.example.presentation.components.security.PinLockOverlay
import com.example.presentation.components.security.SetPinDialog
import com.example.presentation.components.settings.SettingsDialog
import com.example.presentation.components.strategic.StrategicBoardSheet
import com.example.presentation.components.style.StyleBlendDialog
import com.example.presentation.components.style.StyleSelectorBar
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.LocalThemeIsDark
import com.example.ui.theme.SoftCyanGlow
import com.example.ui.theme.SoftVioletGlow
import kotlinx.coroutines.delay

@Composable
fun ConversationScreen(
  modifier: Modifier = Modifier,
  viewModel: ConversationViewModel = viewModel()
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val listState = rememberLazyListState()
  val isDark = LocalThemeIsDark.current
  val clipboardManager = LocalClipboardManager.current

  var previousMessageCount by remember { mutableIntStateOf(uiState.conversation.messages.size) }

  // Auto-dismiss Toast after 2 seconds
  LaunchedEffect(uiState.toastMessage) {
    if (uiState.toastMessage != null) {
      delay(2000)
      viewModel.clearToast()
    }
  }

  // Smart auto-scroll: Scroll down when user sends their own message, or if already at bottom.
  LaunchedEffect(uiState.conversation.messages.size) {
    val currentCount = uiState.conversation.messages.size
    if (currentCount > previousMessageCount && currentCount > 0) {
      val lastMessage = uiState.conversation.messages.lastOrNull()
      val isLastFromUser = lastMessage?.role == MessageRole.USER
      val isAtBottom = !listState.canScrollForward

      if (isLastFromUser || isAtBottom) {
        listState.animateScrollToItem(currentCount - 1)
      }
    }
    previousMessageCount = currentCount
  }

  // Back gestures for modal dialogs and sheets
  BackHandler(enabled = uiState.isRenameDialogOpen) {
    viewModel.dismissRenameDialog()
  }
  BackHandler(enabled = uiState.isSetPinDialogOpen) {
    viewModel.dismissSetPinDialog()
  }
  BackHandler(enabled = uiState.isExportDialogOpen) {
    viewModel.closeExportDialog()
  }
  BackHandler(enabled = uiState.isImportDialogOpen) {
    viewModel.closeImportDialog()
  }
  BackHandler(enabled = uiState.isSettingsOpen && !uiState.isSetPinDialogOpen) {
    viewModel.closeSettings()
  }
  BackHandler(enabled = uiState.isBlendDialogOpen) {
    viewModel.closeBlendDialog()
  }
  BackHandler(enabled = uiState.isStrategicBoardOpen) {
    viewModel.closeStrategicBoard()
  }
  BackHandler(enabled = uiState.isRizzRapideOpen) {
    viewModel.closeRizzRapide()
  }
  BackHandler(
    enabled = uiState.isHistoryOpen &&
      !uiState.isRenameDialogOpen &&
      !uiState.isExportDialogOpen &&
      !uiState.isImportDialogOpen
  ) {
    viewModel.closeHistory()
  }

  // Fluid ambient backdrop
  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .drawBehind {
        if (isDark) {
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(SoftCyanGlow, Color.Transparent),
              center = Offset(size.width * 0.85f, size.height * 0.15f),
              radius = size.width * 0.65f
            ),
            radius = size.width * 0.65f,
            center = Offset(size.width * 0.85f, size.height * 0.15f)
          )
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(SoftVioletGlow, Color.Transparent),
              center = Offset(size.width * 0.15f, size.height * 0.75f),
              radius = size.width * 0.7f
            ),
            radius = size.width * 0.7f,
            center = Offset(size.width * 0.15f, size.height * 0.75f)
          )
        }
      },
    contentAlignment = Alignment.TopCenter
  ) {
    Scaffold(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 640.dp),
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      containerColor = Color.Transparent,
      topBar = {
        ConversationTopBar(
          messageCount = uiState.conversation.messages.size,
          themeMode = uiState.themeMode,
          orbState = uiState.orbState,
          onOpenHistoryClicked = { viewModel.openHistory() },
          onOpenStrategicBoardClicked = { viewModel.openStrategicBoard() },
          onOpenRizzRapideClicked = { viewModel.openRizzRapide() },
          onOpenSettingsClicked = { viewModel.openSettings() },
          onThemeToggleClicked = { viewModel.cycleThemeMode() },
          onClearClicked = { viewModel.clearConversation() }
        )
      },
      bottomBar = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .imePadding()
        ) {
          // Toast Notification Banner
          AnimatedVisibility(
            visible = uiState.toastMessage != null,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it }
          ) {
            val toastText = uiState.toastMessage
            if (toastText != null) {
              Box(
                modifier = Modifier
                  .padding(horizontal = 24.dp, vertical = 4.dp)
                  .align(Alignment.CenterHorizontally)
                  .liquidGlass(
                    shape = RoundedCornerShape(14.dp),
                    isElevated = true,
                    borderWidth = 1.dp
                  )
                  .padding(horizontal = 14.dp, vertical = 6.dp)
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = ElectricBlue,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = toastText,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
              }
            }
          }

          // Generating Indicator
          AnimatedVisibility(
            visible = uiState.isSending,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
          ) {
            Row(
              modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 4.dp)
                .fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = ElectricBlue
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = stringResource(R.string.generating_indicator),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }

          // Error and Retry Banner
          AnimatedVisibility(
            visible = uiState.errorMessage != null,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
          ) {
            val errorText = uiState.errorMessage
            if (errorText != null) {
              Box(
                modifier = Modifier
                  .padding(horizontal = 16.dp, vertical = 4.dp)
                  .fillMaxWidth()
                  .liquidGlass(
                    shape = RoundedCornerShape(16.dp),
                    isElevated = true,
                    borderWidth = 1.dp
                  )
                  .padding(12.dp)
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.WarningAmber,
                      contentDescription = null,
                      tint = Color(0xFFF43F5E),
                      modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = errorText,
                      style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                      color = MaterialTheme.colorScheme.onSurface
                    )
                  }

                  Row(verticalAlignment = Alignment.CenterVertically) {
                    if (uiState.canRetry) {
                      OutlinedButton(
                        onClick = { viewModel.retryLastMessage() },
                        modifier = Modifier.testTag("retry_button"),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                          contentColor = MaterialTheme.colorScheme.primary
                        )
                      ) {
                        Icon(
                          imageVector = Icons.Outlined.Refresh,
                          contentDescription = null,
                          modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                          text = stringResource(R.string.retry_button),
                          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                      }
                    }

                    IconButton(
                      onClick = { viewModel.clearErrorMessage() },
                      modifier = Modifier.size(36.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Fermer l'erreur",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                      )
                    }
                  }
                }
              }
            }
          }

          // Style Selector Bar
          StyleSelectorBar(
            styleSettings = uiState.styleSettings,
            onStyleSelected = { viewModel.selectStyle(it) },
            onIntensityChanged = { viewModel.setStyleIntensity(it) },
            onOpenBlendDialog = { viewModel.openBlendDialog() }
          )

          // Main Input Field
          ConversationInputField(
            inputText = uiState.inputText,
            attachedImage = uiState.attachedImage,
            isSending = uiState.isSending,
            onTextChanged = { viewModel.onInputTextChanged(it) },
            onImageAttached = { viewModel.onImageSelected(it) },
            onRemoveImage = { viewModel.onRemoveAttachedImage() },
            onSendClicked = { viewModel.sendMessage() }
          )
        }
      }
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        if (uiState.conversation.messages.isEmpty()) {
          ConversationEmptyState(
            orbState = uiState.orbState,
            onSuggestionClick = { suggestion ->
              viewModel.onSelectSuggestion(suggestion)
            },
            modifier = Modifier.fillMaxSize()
          )
        } else {
          LazyColumn(
            state = listState,
            contentPadding = PaddingValues(vertical = 12.dp),
            modifier = Modifier.fillMaxSize()
          ) {
            items(
              items = uiState.conversation.messages,
              key = { it.id }
            ) { message ->
              MessageBubble(
                message = message,
                onCopyClicked = { text ->
                  clipboardManager.setText(AnnotatedString(text))
                  viewModel.showToast("Réponse copiée dans le presse-papier !")
                },
                onRegenerateClicked = { id ->
                  viewModel.regenerateAssistantMessage(id)
                },
                onReuseClicked = { text ->
                  viewModel.reuseAssistantMessage(text)
                }
              )
            }
          }
        }
      }
    }

    // Modal Style Blend Dialog
    if (uiState.isBlendDialogOpen) {
      StyleBlendDialog(
        styleSettings = uiState.styleSettings,
        onToggleBlendMode = { viewModel.setBlendModeEnabled(it) },
        onWeightChanged = { style, weight -> viewModel.updateStyleWeight(style, weight) },
        onResetWeights = { viewModel.resetWeights() },
        onDismiss = { viewModel.closeBlendDialog() }
      )
    }

    // Modal Rizz Rapide Sheet
    if (uiState.isRizzRapideOpen) {
      RizzRapideSheet(
        situationText = uiState.rizzRapideSituation,
        options = uiState.rizzRapideOptions,
        isGenerating = uiState.isGeneratingRizzRapide,
        errorMessage = uiState.rizzRapideError,
        styleSettings = uiState.styleSettings,
        onSituationChanged = { viewModel.updateRizzRapideSituation(it) },
        onGenerateClicked = { viewModel.generateRizzRapide() },
        onOptionSelected = { option -> viewModel.useRizzRapideOption(option) },
        onSendDirectlyClicked = { option -> viewModel.sendRizzRapideOptionDirectly(option) },
        onCopiedToast = { msg -> viewModel.showToast(msg) },
        onDismiss = { viewModel.closeRizzRapide() }
      )
    }

    // Modal Échiquier conversationnel stratégique & Mode Coach
    if (uiState.isStrategicBoardOpen) {
      StrategicBoardSheet(
        situationText = uiState.strategicSituation,
        boardResult = uiState.strategicBoardResult,
        isGenerating = uiState.isGeneratingStrategicBoard,
        errorMessage = uiState.strategicBoardError,
        isCoachModeActive = uiState.isCoachModeActive,
        selectedOptionId = uiState.selectedStrategicOptionId,
        realReplyInput = uiState.realReceivedReplyInput,
        isAnalyzingRealReply = uiState.isAnalyzingRealReply,
        comparisonResult = uiState.comparisonResult,
        onSituationChanged = { viewModel.updateStrategicSituation(it) },
        onGenerateClicked = { viewModel.generateStrategicBoard() },
        onToggleCoachMode = { viewModel.toggleCoachMode(it) },
        onSelectOption = { viewModel.selectStrategicOption(it) },
        onRealReplyChanged = { viewModel.updateRealReceivedReply(it) },
        onAnalyzeRealReply = { viewModel.analyzeRealReceivedReply() },
        onUseReply = { viewModel.useStrategicReply(it) },
        onSendReplyDirectly = { viewModel.sendStrategicReplyDirectly(it) },
        onCopiedToast = { viewModel.showToast(it) },
        onDismiss = { viewModel.closeStrategicBoard() }
      )
    }

    // Modal Conversation History Sheet
    if (uiState.isHistoryOpen) {
      ConversationHistorySheet(
        conversationList = uiState.conversationList,
        searchQuery = uiState.historySearchQuery,
        currentConversationId = uiState.conversation.id,
        onSearchQueryChanged = { viewModel.updateHistorySearchQuery(it) },
        onNewConversationClicked = { viewModel.createNewConversation() },
        onSelectConversation = { viewModel.switchConversation(it) },
        onRenameClicked = { id, title -> viewModel.openRenameDialog(id, title) },
        onDeleteClicked = { viewModel.deleteConversation(it) },
        onOpenExportClicked = { viewModel.openExportDialog() },
        onOpenImportClicked = { viewModel.openImportDialog() },
        onDismiss = { viewModel.closeHistory() }
      )
    }

    // Modal Settings & Privacy Dialog
    if (uiState.isSettingsOpen) {
      SettingsDialog(
        themeMode = uiState.themeMode,
        language = uiState.appLanguage,
        isPinLockEnabled = uiState.isPinLockEnabled,
        onThemeModeSelected = { viewModel.setThemeMode(it) },
        onLanguageSelected = { viewModel.setAppLanguage(it) },
        onOpenSetPinClicked = { viewModel.openSetPinDialog() },
        onDisablePinClicked = { viewModel.disablePinLock() },
        onDeleteAllDataClicked = { viewModel.deleteAllData() },
        onDismiss = { viewModel.closeSettings() }
      )
    }

    // Rename Conversation Dialog
    if (uiState.isRenameDialogOpen) {
      RenameDialog(
        currentTitle = uiState.renamingConversationTitle,
        onTitleChanged = { viewModel.updateRenamingTitle(it) },
        onConfirm = { viewModel.confirmRenameConversation() },
        onDismiss = { viewModel.dismissRenameDialog() }
      )
    }

    // Set PIN Dialog
    if (uiState.isSetPinDialogOpen) {
      SetPinDialog(
        currentInput = uiState.newPinInput,
        onDigitClicked = { viewModel.updateNewPinInput(it) },
        onBackspaceClicked = { viewModel.removeNewPinDigit() },
        onConfirm = { viewModel.confirmSetPin() },
        onDismiss = { viewModel.dismissSetPinDialog() }
      )
    }

    // Export JSON Dialog
    if (uiState.isExportDialogOpen && uiState.exportJsonData != null) {
      ExportDialog(
        jsonData = uiState.exportJsonData ?: "",
        onDismiss = { viewModel.closeExportDialog() },
        onCopiedToast = { viewModel.showToast("Données JSON copiées !") }
      )
    }

    // Import JSON Dialog
    if (uiState.isImportDialogOpen) {
      ImportDialog(
        inputJson = uiState.importJsonInput,
        errorMessage = uiState.importError,
        onInputChanged = { viewModel.updateImportJsonInput(it) },
        onConfirmImport = { viewModel.confirmImportJson() },
        onDismiss = { viewModel.closeImportDialog() }
      )
    }

    // PIN Gate Lock Overlay (blocks interaction until PIN is unlocked)
    if (uiState.isPinLockEnabled && uiState.isPinGateLocked) {
      PinLockOverlay(
        pinInput = uiState.pinKeypadInput,
        isError = uiState.pinError,
        onDigitClicked = { viewModel.onPinDigitEntered(it) },
        onBackspaceClicked = { viewModel.onPinBackspace() }
      )
    }
  }
}
