package com.example.domain.usecase

import com.example.domain.model.AiError
import com.example.domain.model.AiResult
import com.example.domain.model.StyleSettings
import com.example.domain.repository.AiRepository

class RizzRapideUseCase(
  private val aiRepository: AiRepository
) {
  suspend operator fun invoke(
    situation: String,
    styleSettings: StyleSettings = StyleSettings()
  ): AiResult<List<String>> {
    val trimmed = situation.trim()
    if (trimmed.isBlank()) {
      return AiResult.Error(AiError.Unknown("Veuillez saisir une situation ou une phrase reçue."))
    }

    return aiRepository.generateRizzRapide(
      situation = trimmed,
      styleSettings = styleSettings
    )
  }
}
