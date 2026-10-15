package com.example.domain.usecase

import com.example.domain.model.AiError
import com.example.domain.model.AiResult
import com.example.domain.model.AttachedImage
import com.example.domain.model.Conversation
import com.example.domain.model.StyleSettings
import com.example.domain.repository.AiRepository

class SendMessageUseCase(
  private val aiRepository: AiRepository
) {
  suspend operator fun invoke(
    conversation: Conversation,
    userPrompt: String,
    attachedImage: AttachedImage?,
    styleSettings: StyleSettings = StyleSettings()
  ): AiResult<String> {
    val trimmedPrompt = userPrompt.trim()
    if (trimmedPrompt.isBlank() && attachedImage == null) {
      return AiResult.Error(AiError.Unknown("Le message ou la pièce jointe ne peut pas être vide."))
    }

    return aiRepository.generateResponse(
      conversation = conversation,
      userPrompt = trimmedPrompt,
      attachedImage = attachedImage,
      styleSettings = styleSettings
    )
  }
}
