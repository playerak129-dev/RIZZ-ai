package com.example.domain.usecase

import com.example.domain.model.AiResult
import com.example.domain.model.StyleSettings
import com.example.domain.model.strategic.StrategicBoardResult
import com.example.domain.repository.AiRepository

class GenerateStrategicBoardUseCase(
  private val aiRepository: AiRepository
) {
  suspend operator fun invoke(
    situation: String,
    styleSettings: StyleSettings = StyleSettings()
  ): AiResult<StrategicBoardResult> {
    val cleanSituation = situation.trim()
    if (cleanSituation.isBlank()) {
      return AiResult.Error(com.example.domain.model.AiError.Unknown("Le contexte ou message reçu est requis."))
    }
    return aiRepository.generateStrategicBoard(cleanSituation, styleSettings)
  }
}
