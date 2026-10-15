package com.example.presentation.conversation

import com.example.domain.model.AttachedImage
import com.example.domain.model.Conversation
import com.example.domain.model.ConversationSummary
import com.example.domain.model.StyleSettings
import com.example.domain.model.strategic.ComparisonResult
import com.example.domain.model.strategic.StrategicBoardResult
import com.example.presentation.components.orb.OrbState
import com.example.ui.theme.ThemeMode

data class ConversationUiState(
  val conversation: Conversation = Conversation(),
  val conversationList: List<ConversationSummary> = emptyList(),
  val inputText: String = "",
  val attachedImage: AttachedImage? = null,
  val isSending: Boolean = false,
  val themeMode: ThemeMode = ThemeMode.SYSTEM,
  val orbState: OrbState = OrbState.IDLE,
  val errorMessage: String? = null,
  val canRetry: Boolean = false,
  val statusNote: String = "C4RL3NTZ Edition • RIZZ AI 2.0",
  val styleSettings: StyleSettings = StyleSettings(),
  val isBlendDialogOpen: Boolean = false,

  // Rizz Rapide
  val isRizzRapideOpen: Boolean = false,
  val rizzRapideSituation: String = "",
  val rizzRapideOptions: List<String> = emptyList(),
  val isGeneratingRizzRapide: Boolean = false,
  val rizzRapideError: String? = null,

  // Échiquier conversationnel stratégique & Mode Coach
  val isStrategicBoardOpen: Boolean = false,
  val strategicSituation: String = "",
  val isGeneratingStrategicBoard: Boolean = false,
  val strategicBoardResult: StrategicBoardResult? = null,
  val strategicBoardError: String? = null,
  val isCoachModeActive: Boolean = false,
  val selectedStrategicOptionId: String? = null,
  val realReceivedReplyInput: String = "",
  val isAnalyzingRealReply: Boolean = false,
  val comparisonResult: ComparisonResult? = null,

  // History & Management
  val isHistoryOpen: Boolean = false,
  val historySearchQuery: String = "",
  val isRenameDialogOpen: Boolean = false,
  val renamingConversationId: String? = null,
  val renamingConversationTitle: String = "",

  // Settings & Security
  val isSettingsOpen: Boolean = false,
  val appLanguage: String = "fr",
  val isPinLockEnabled: Boolean = false,
  val isPinGateLocked: Boolean = false,
  val pinKeypadInput: String = "",
  val pinError: Boolean = false,
  val isSetPinDialogOpen: Boolean = false,
  val newPinInput: String = "",

  // Export / Import
  val isExportDialogOpen: Boolean = false,
  val exportJsonData: String? = null,
  val isImportDialogOpen: Boolean = false,
  val importJsonInput: String = "",
  val importError: String? = null,

  // Notification Toast
  val toastMessage: String? = null
)
