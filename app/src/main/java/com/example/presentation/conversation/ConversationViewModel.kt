package com.example.presentation.conversation

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.SecurityUtils
import com.example.data.local.db.AppDatabase
import com.example.data.local.preferences.AppPreferencesRepository
import com.example.data.repository.ConversationRepository
import com.example.data.repository.GeminiAiRepository
import com.example.data.repository.InMemoryConversationRepository
import com.example.data.repository.RoomConversationRepository
import com.example.domain.model.AiResult
import com.example.domain.model.AttachedImage
import com.example.domain.model.ConversationStyle
import com.example.domain.model.Message
import com.example.domain.model.MessageRole
import com.example.domain.model.StyleSettings
import com.example.domain.usecase.AnalyzeRealReplyUseCase
import com.example.domain.usecase.GenerateStrategicBoardUseCase
import com.example.domain.usecase.RizzRapideUseCase
import com.example.domain.usecase.SendMessageUseCase
import com.example.presentation.components.orb.OrbState
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ConversationViewModel(
  private val repository: ConversationRepository = InMemoryConversationRepository(),
  private val sendMessageUseCase: SendMessageUseCase = SendMessageUseCase(GeminiAiRepository()),
  private val rizzRapideUseCase: RizzRapideUseCase = RizzRapideUseCase(GeminiAiRepository()),
  private val strategicBoardUseCase: GenerateStrategicBoardUseCase = GenerateStrategicBoardUseCase(GeminiAiRepository()),
  private val analyzeRealReplyUseCase: AnalyzeRealReplyUseCase = AnalyzeRealReplyUseCase(GeminiAiRepository()),
  private val preferencesRepository: AppPreferencesRepository? = null
) : ViewModel() {

  constructor(application: Application) : this(
    repository = RoomConversationRepository(AppDatabase.getInstance(application)),
    sendMessageUseCase = SendMessageUseCase(GeminiAiRepository()),
    rizzRapideUseCase = RizzRapideUseCase(GeminiAiRepository()),
    strategicBoardUseCase = GenerateStrategicBoardUseCase(GeminiAiRepository()),
    analyzeRealReplyUseCase = AnalyzeRealReplyUseCase(GeminiAiRepository()),
    preferencesRepository = AppPreferencesRepository(application)
  )

  private val _uiState = MutableStateFlow(ConversationUiState())
  val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()

  private var activeGenerationJob: Job? = null
  private var rizzRapideJob: Job? = null
  private var strategicBoardJob: Job? = null
  private var lastFailedPrompt: String? = null
  private var lastFailedImage: AttachedImage? = null

  init {
    // 1. Observe active conversation
    viewModelScope.launch {
      repository.observeConversation().collect { conversation ->
        _uiState.update { current ->
          current.copy(conversation = conversation)
        }
      }
    }

    // 2. Observe all conversations for history list
    viewModelScope.launch {
      repository.observeAllConversationsSummary().collect { list ->
        _uiState.update { it.copy(conversationList = list) }
      }
    }

    // 3. Load DataStore preferences if available
    if (preferencesRepository != null) {
      viewModelScope.launch {
        preferencesRepository.themeModeFlow.collect { mode ->
          _uiState.update { it.copy(themeMode = mode) }
        }
      }
      viewModelScope.launch {
        preferencesRepository.languageFlow.collect { lang ->
          _uiState.update { it.copy(appLanguage = lang) }
        }
      }
      viewModelScope.launch {
        preferencesRepository.isPinLockEnabledFlow.collect { isEnabled ->
          _uiState.update {
            it.copy(
              isPinLockEnabled = isEnabled,
              isPinGateLocked = isEnabled
            )
          }
        }
      }
      viewModelScope.launch {
        val initialStyle = preferencesRepository.defaultStyleFlow.firstOrNull() ?: ConversationStyle.NEUTRE
        val initialIntensity = preferencesRepository.defaultIntensityFlow.firstOrNull() ?: 3
        _uiState.update { current ->
          current.copy(
            styleSettings = current.styleSettings.copy(
              activeStyle = initialStyle,
              intensity = initialIntensity
            )
          )
        }
      }
    }
  }

  // --- Style & Intensity Controls ---

  fun selectStyle(style: ConversationStyle) {
    _uiState.update { current ->
      current.copy(
        styleSettings = current.styleSettings.copy(
          activeStyle = style,
          isBlendModeEnabled = false
        )
      )
    }
    persistStyleSettings()
  }

  fun setStyleIntensity(intensity: Int) {
    val clamped = intensity.coerceIn(1, 5)
    _uiState.update { current ->
      current.copy(
        styleSettings = current.styleSettings.copy(intensity = clamped)
      )
    }
    persistStyleSettings()
  }

  fun setBlendModeEnabled(enabled: Boolean) {
    _uiState.update { current ->
      current.copy(
        styleSettings = current.styleSettings.copy(isBlendModeEnabled = enabled)
      )
    }
    persistStyleSettings()
  }

  fun updateStyleWeight(style: ConversationStyle, newWeight: Int) {
    _uiState.update { current ->
      current.copy(
        styleSettings = current.styleSettings.withAdjustedWeight(style, newWeight)
      )
    }
    persistStyleSettings()
  }

  fun resetWeights() {
    _uiState.update { current ->
      current.copy(
        styleSettings = current.styleSettings.withResetWeights()
      )
    }
    persistStyleSettings()
  }

  private fun persistStyleSettings() {
    viewModelScope.launch {
      val settings = _uiState.value.styleSettings
      repository.saveCurrentStyleSettings(settings)
      preferencesRepository?.setDefaultStyle(settings.activeStyle)
      preferencesRepository?.setDefaultIntensity(settings.intensity)
    }
  }

  fun openBlendDialog() {
    _uiState.update { it.copy(isBlendDialogOpen = true) }
  }

  fun closeBlendDialog() {
    _uiState.update { it.copy(isBlendDialogOpen = false) }
  }

  // --- Input & Photo Controls ---

  fun onInputTextChanged(text: String) {
    _uiState.update { current ->
      current.copy(
        inputText = text,
        orbState = if (current.isSending) {
          OrbState.THINKING
        } else if (text.isNotBlank() || current.attachedImage != null) {
          OrbState.FOCUSED
        } else {
          OrbState.IDLE
        }
      )
    }
  }

  fun onImageSelected(image: AttachedImage?) {
    _uiState.update { current ->
      current.copy(
        attachedImage = image,
        orbState = if (current.isSending) OrbState.THINKING else if (image != null) OrbState.FOCUSED else OrbState.IDLE
      )
    }
  }

  fun onRemoveAttachedImage() {
    _uiState.update { current ->
      current.copy(
        attachedImage = null,
        orbState = if (current.inputText.isNotBlank()) OrbState.FOCUSED else OrbState.IDLE
      )
    }
  }

  // --- Theme, Toast, Status & Orb Controls ---

  fun setThemeMode(mode: ThemeMode) {
    _uiState.update { it.copy(themeMode = mode) }
    viewModelScope.launch {
      preferencesRepository?.setThemeMode(mode)
    }
  }

  fun cycleThemeMode() {
    val nextMode = _uiState.value.themeMode.next()
    setThemeMode(nextMode)
  }

  fun setOrbState(state: OrbState) {
    _uiState.update { it.copy(orbState = state) }
  }

  fun clearErrorMessage() {
    _uiState.update { it.copy(errorMessage = null, canRetry = false) }
  }

  fun showToast(message: String) {
    _uiState.update { it.copy(toastMessage = message) }
  }

  fun clearToast() {
    _uiState.update { it.copy(toastMessage = null) }
  }

  // --- Message Sending, Regeneration & Reuse ---

  fun sendMessage() {
    val currentState = _uiState.value
    if (currentState.isSending) return

    val textToSend = currentState.inputText.trim()
    val imageToSend = currentState.attachedImage

    if (textToSend.isBlank() && imageToSend == null) return

    executeSendMessage(textToSend, imageToSend)
  }

  fun retryLastMessage() {
    val prompt = lastFailedPrompt ?: return
    val image = lastFailedImage
    _uiState.update { it.copy(errorMessage = null, canRetry = false) }
    executeSendMessage(prompt, image, isRetry = true)
  }

  fun regenerateAssistantMessage(assistantMessageId: String) {
    val currentMessages = _uiState.value.conversation.messages
    val assistantIndex = currentMessages.indexOfFirst { it.id == assistantMessageId }
    if (assistantIndex == -1) return

    val userMessage = currentMessages
      .take(assistantIndex)
      .lastOrNull { it.role == MessageRole.USER }
      ?: return

    executeSendMessage(
      textToSend = userMessage.content,
      imageToSend = null,
      isRetry = true
    )
  }

  fun reuseAssistantMessage(text: String) {
    _uiState.update {
      it.copy(
        inputText = text,
        orbState = OrbState.FOCUSED
      )
    }
    showToast("Réponse placée dans le champ de saisie !")
  }

  private fun executeSendMessage(
    textToSend: String,
    imageToSend: AttachedImage?,
    isRetry: Boolean = false
  ) {
    activeGenerationJob?.cancel()
    activeGenerationJob = viewModelScope.launch {
      if (!isRetry) {
        val userMessage = Message(
          role = MessageRole.USER,
          content = textToSend,
          imageUri = imageToSend?.uri
        )
        repository.addMessage(userMessage)
      }

      lastFailedPrompt = textToSend
      lastFailedImage = imageToSend

      _uiState.update {
        it.copy(
          inputText = "",
          attachedImage = null,
          isSending = true,
          errorMessage = null,
          canRetry = false,
          orbState = OrbState.THINKING
        )
      }

      val currentConversation = repository.getCurrentConversation()
      val currentStyleSettings = _uiState.value.styleSettings

      val result = sendMessageUseCase(
        conversation = currentConversation,
        userPrompt = textToSend,
        attachedImage = imageToSend,
        styleSettings = currentStyleSettings
      )

      when (result) {
        is AiResult.Success -> {
          val assistantMessage = Message(
            role = MessageRole.ASSISTANT,
            content = result.data
          )
          repository.addMessage(assistantMessage)
          lastFailedPrompt = null
          lastFailedImage = null

          _uiState.update {
            it.copy(
              isSending = false,
              errorMessage = null,
              canRetry = false,
              orbState = OrbState.SUCCESS
            )
          }

          delay(1200)
          _uiState.update { current ->
            if (current.orbState == OrbState.SUCCESS) current.copy(orbState = OrbState.IDLE) else current
          }
        }
        is AiResult.Error -> {
          _uiState.update {
            it.copy(
              isSending = false,
              errorMessage = result.error.message,
              canRetry = true,
              orbState = OrbState.ERROR
            )
          }

          delay(2500)
          _uiState.update { current ->
            if (current.orbState == OrbState.ERROR) current.copy(orbState = OrbState.IDLE) else current
          }
        }
      }
    }
  }

  // --- Rizz Rapide Operations ---

  fun openRizzRapide() {
    _uiState.update { it.copy(isRizzRapideOpen = true, rizzRapideError = null) }
  }

  fun closeRizzRapide() {
    rizzRapideJob?.cancel()
    _uiState.update { it.copy(isRizzRapideOpen = false, isGeneratingRizzRapide = false) }
  }

  fun updateRizzRapideSituation(situation: String) {
    _uiState.update { it.copy(rizzRapideSituation = situation, rizzRapideError = null) }
  }

  fun generateRizzRapide() {
    val situation = _uiState.value.rizzRapideSituation.trim()
    if (situation.isBlank()) return

    rizzRapideJob?.cancel()
    rizzRapideJob = viewModelScope.launch {
      _uiState.update {
        it.copy(
          isGeneratingRizzRapide = true,
          rizzRapideError = null,
          orbState = OrbState.THINKING
        )
      }

      val result = rizzRapideUseCase(
        situation = situation,
        styleSettings = _uiState.value.styleSettings
      )

      when (result) {
        is AiResult.Success -> {
          _uiState.update {
            it.copy(
              isGeneratingRizzRapide = false,
              rizzRapideOptions = result.data,
              rizzRapideError = null,
              orbState = OrbState.SUCCESS
            )
          }
          delay(1000)
          _uiState.update { current ->
            if (current.orbState == OrbState.SUCCESS) current.copy(orbState = OrbState.IDLE) else current
          }
        }
        is AiResult.Error -> {
          _uiState.update {
            it.copy(
              isGeneratingRizzRapide = false,
              rizzRapideError = result.error.message,
              orbState = OrbState.ERROR
            )
          }
          delay(2000)
          _uiState.update { current ->
            if (current.orbState == OrbState.ERROR) current.copy(orbState = OrbState.IDLE) else current
          }
        }
      }
    }
  }

  fun useRizzRapideOption(option: String) {
    _uiState.update {
      it.copy(
        inputText = option,
        isRizzRapideOpen = false,
        orbState = OrbState.FOCUSED
      )
    }
    showToast("Proposition insérée dans la discussion !")
  }

  fun sendRizzRapideOptionDirectly(option: String) {
    _uiState.update { it.copy(isRizzRapideOpen = false) }
    executeSendMessage(textToSend = option, imageToSend = null)
  }

  fun onSelectSuggestion(suggestion: String) {
    _uiState.update {
      it.copy(
        inputText = suggestion,
        orbState = OrbState.FOCUSED
      )
    }
  }

  // --- Échiquier Conversationnel Stratégique & Mode Coach ---

  fun openStrategicBoard() {
    _uiState.update {
      it.copy(
        isStrategicBoardOpen = true,
        strategicBoardError = null
      )
    }
  }

  fun closeStrategicBoard() {
    strategicBoardJob?.cancel()
    _uiState.update {
      it.copy(
        isStrategicBoardOpen = false,
        isGeneratingStrategicBoard = false,
        isAnalyzingRealReply = false
      )
    }
  }

  fun updateStrategicSituation(text: String) {
    _uiState.update { it.copy(strategicSituation = text, strategicBoardError = null) }
  }

  fun toggleCoachMode(active: Boolean) {
    _uiState.update { it.copy(isCoachModeActive = active) }
  }

  fun selectStrategicOption(optionId: String) {
    _uiState.update { it.copy(selectedStrategicOptionId = optionId) }
  }

  fun updateRealReceivedReply(text: String) {
    _uiState.update { it.copy(realReceivedReplyInput = text) }
  }

  fun generateStrategicBoard() {
    val situation = _uiState.value.strategicSituation.trim()
    if (situation.isBlank()) return

    strategicBoardJob?.cancel()
    strategicBoardJob = viewModelScope.launch {
      _uiState.update {
        it.copy(
          isGeneratingStrategicBoard = true,
          strategicBoardError = null,
          orbState = OrbState.THINKING
        )
      }

      val result = strategicBoardUseCase(
        situation = situation,
        styleSettings = _uiState.value.styleSettings
      )

      when (result) {
        is AiResult.Success -> {
          val firstOptionId = result.data.options.firstOrNull { it.isRecommended }?.id
            ?: result.data.options.firstOrNull()?.id
          _uiState.update {
            it.copy(
              isGeneratingStrategicBoard = false,
              strategicBoardResult = result.data,
              selectedStrategicOptionId = firstOptionId,
              strategicBoardError = null,
              comparisonResult = null,
              orbState = OrbState.SUCCESS
            )
          }
          delay(1000)
          _uiState.update { current ->
            if (current.orbState == OrbState.SUCCESS) current.copy(orbState = OrbState.IDLE) else current
          }
        }
        is AiResult.Error -> {
          _uiState.update {
            it.copy(
              isGeneratingStrategicBoard = false,
              strategicBoardError = result.error.message,
              orbState = OrbState.ERROR
            )
          }
          delay(2000)
          _uiState.update { current ->
            if (current.orbState == OrbState.ERROR) current.copy(orbState = OrbState.IDLE) else current
          }
        }
      }
    }
  }

  fun analyzeRealReceivedReply() {
    val state = _uiState.value
    val realReply = state.realReceivedReplyInput.trim()
    if (realReply.isBlank()) return

    val currentBoard = state.strategicBoardResult ?: return
    val chosenOption = currentBoard.options.firstOrNull { it.id == state.selectedStrategicOptionId }?.proposedReply
      ?: currentBoard.options.firstOrNull()?.proposedReply ?: ""

    viewModelScope.launch {
      _uiState.update { it.copy(isAnalyzingRealReply = true) }
      val result = analyzeRealReplyUseCase(
        situation = state.strategicSituation,
        chosenOption = chosenOption,
        realReceivedReply = realReply
      )

      when (result) {
        is AiResult.Success -> {
          _uiState.update {
            it.copy(
              isAnalyzingRealReply = false,
              comparisonResult = result.data
            )
          }
        }
        is AiResult.Error -> {
          _uiState.update {
            it.copy(
              isAnalyzingRealReply = false,
              strategicBoardError = result.error.message
            )
          }
        }
      }
    }
  }

  fun useStrategicReply(replyText: String) {
    _uiState.update {
      it.copy(
        inputText = replyText,
        isStrategicBoardOpen = false,
        orbState = OrbState.FOCUSED
      )
    }
    showToast("Réponse stratégique insérée dans le chat !")
  }

  fun sendStrategicReplyDirectly(replyText: String) {
    _uiState.update { it.copy(isStrategicBoardOpen = false) }
    executeSendMessage(textToSend = replyText, imageToSend = null)
  }

  // --- History & Conversation Management ---

  fun openHistory() {
    _uiState.update { it.copy(isHistoryOpen = true, historySearchQuery = "") }
  }

  fun closeHistory() {
    _uiState.update { it.copy(isHistoryOpen = false) }
  }

  fun updateHistorySearchQuery(query: String) {
    _uiState.update { it.copy(historySearchQuery = query) }
  }

  fun createNewConversation(title: String = "Nouvelle discussion") {
    viewModelScope.launch {
      repository.createNewConversation(title, _uiState.value.styleSettings)
      _uiState.update {
        it.copy(
          inputText = "",
          attachedImage = null,
          isHistoryOpen = false,
          errorMessage = null
        )
      }
      showToast("Nouvelle discussion créée !")
    }
  }

  fun switchConversation(id: String) {
    viewModelScope.launch {
      repository.switchConversation(id)
      _uiState.update {
        it.copy(
          isHistoryOpen = false,
          inputText = "",
          attachedImage = null,
          errorMessage = null
        )
      }
    }
  }

  fun openRenameDialog(conversationId: String, currentTitle: String) {
    _uiState.update {
      it.copy(
        isRenameDialogOpen = true,
        renamingConversationId = conversationId,
        renamingConversationTitle = currentTitle
      )
    }
  }

  fun updateRenamingTitle(title: String) {
    _uiState.update { it.copy(renamingConversationTitle = title) }
  }

  fun dismissRenameDialog() {
    _uiState.update {
      it.copy(
        isRenameDialogOpen = false,
        renamingConversationId = null,
        renamingConversationTitle = ""
      )
    }
  }

  fun confirmRenameConversation() {
    val id = _uiState.value.renamingConversationId ?: return
    val title = _uiState.value.renamingConversationTitle.trim()
    if (title.isBlank()) return

    viewModelScope.launch {
      repository.renameConversation(id, title)
      dismissRenameDialog()
      showToast("Conversation renommée !")
    }
  }

  fun deleteConversation(id: String) {
    viewModelScope.launch {
      repository.deleteConversation(id)
      showToast("Conversation supprimée.")
    }
  }

  fun clearConversation() {
    activeGenerationJob?.cancel()
    rizzRapideJob?.cancel()
    strategicBoardJob?.cancel()
    viewModelScope.launch {
      repository.clearConversation()
      lastFailedPrompt = null
      lastFailedImage = null
      _uiState.update {
        it.copy(
          inputText = "",
          attachedImage = null,
          isSending = false,
          errorMessage = null,
          canRetry = false,
          orbState = OrbState.IDLE,
          rizzRapideOptions = emptyList()
        )
      }
      showToast("Discussion effacée.")
    }
  }

  // --- Settings & Security ---

  fun openSettings() {
    _uiState.update { it.copy(isSettingsOpen = true) }
  }

  fun closeSettings() {
    _uiState.update { it.copy(isSettingsOpen = false) }
  }

  fun setAppLanguage(lang: String) {
    _uiState.update { it.copy(appLanguage = lang) }
    viewModelScope.launch {
      preferencesRepository?.setLanguage(lang)
      showToast("Langue mise à jour : ${if (lang == "fr") "Français" else "English"}")
    }
  }

  // PIN Lock Management
  fun openSetPinDialog() {
    _uiState.update { it.copy(isSetPinDialogOpen = true, newPinInput = "") }
  }

  fun updateNewPinInput(digit: String) {
    val current = _uiState.value.newPinInput
    if (current.length < 4) {
      _uiState.update { it.copy(newPinInput = current + digit) }
    }
  }

  fun removeNewPinDigit() {
    val current = _uiState.value.newPinInput
    if (current.isNotEmpty()) {
      _uiState.update { it.copy(newPinInput = current.dropLast(1)) }
    }
  }

  fun dismissSetPinDialog() {
    _uiState.update { it.copy(isSetPinDialogOpen = false, newPinInput = "") }
  }

  fun confirmSetPin() {
    val pin = _uiState.value.newPinInput
    if (pin.length == 4) {
      viewModelScope.launch {
        preferencesRepository?.setPinLock(enabled = true, rawPin = pin)
        _uiState.update { it.copy(isPinLockEnabled = true, isSetPinDialogOpen = false, newPinInput = "") }
        showToast("Verrouillage par code PIN activé !")
      }
    }
  }

  fun disablePinLock() {
    viewModelScope.launch {
      preferencesRepository?.setPinLock(enabled = false)
      _uiState.update { it.copy(isPinLockEnabled = false, isPinGateLocked = false) }
      showToast("Verrouillage désactivé.")
    }
  }

  // PIN Gate Keypad
  fun onPinDigitEntered(digit: String) {
    val current = _uiState.value.pinKeypadInput
    if (current.length < 4) {
      val updated = current + digit
      _uiState.update { it.copy(pinKeypadInput = updated, pinError = false) }
      if (updated.length == 4) {
        verifyPinKeypad(updated)
      }
    }
  }

  fun onPinBackspace() {
    val current = _uiState.value.pinKeypadInput
    if (current.isNotEmpty()) {
      _uiState.update { it.copy(pinKeypadInput = current.dropLast(1), pinError = false) }
    }
  }

  private fun verifyPinKeypad(inputPin: String) {
    viewModelScope.launch {
      val storedHash = preferencesRepository?.pinHashFlow?.firstOrNull()
      val isMatch = SecurityUtils.verifyPin(inputPin, storedHash)
      if (isMatch) {
        _uiState.update { it.copy(isPinGateLocked = false, pinKeypadInput = "", pinError = false) }
      } else {
        _uiState.update { it.copy(pinKeypadInput = "", pinError = true) }
        showToast("Code PIN incorrect.")
      }
    }
  }

  // Purge all data
  fun deleteAllData() {
    viewModelScope.launch {
      repository.deleteAllData()
      preferencesRepository?.clearAllPreferences()
      _uiState.update {
        ConversationUiState(
          toastMessage = "Toutes les données ont été supprimées."
        )
      }
    }
  }

  // --- Export / Import JSON ---

  fun openExportDialog() {
    viewModelScope.launch {
      val jsonData = repository.exportToJson()
      _uiState.update { it.copy(isExportDialogOpen = true, exportJsonData = jsonData) }
    }
  }

  fun closeExportDialog() {
    _uiState.update { it.copy(isExportDialogOpen = false, exportJsonData = null) }
  }

  fun openImportDialog() {
    _uiState.update { it.copy(isImportDialogOpen = true, importJsonInput = "", importError = null) }
  }

  fun closeImportDialog() {
    _uiState.update { it.copy(isImportDialogOpen = false, importJsonInput = "", importError = null) }
  }

  fun updateImportJsonInput(text: String) {
    _uiState.update { it.copy(importJsonInput = text, importError = null) }
  }

  fun confirmImportJson() {
    val json = _uiState.value.importJsonInput.trim()
    if (json.isBlank()) {
      _uiState.update { it.copy(importError = "Veuillez coller le JSON à importer.") }
      return
    }

    viewModelScope.launch {
      val result = repository.importFromJson(json)
      result.fold(
        onSuccess = { count ->
          closeImportDialog()
          showToast("$count conversation(s) importée(s) avec succès !")
        },
        onFailure = { error ->
          _uiState.update { it.copy(importError = error.message) }
        }
      )
    }
  }
}
