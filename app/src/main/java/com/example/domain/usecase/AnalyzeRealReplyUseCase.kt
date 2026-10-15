package com.example.domain.usecase

import com.example.domain.model.AiError
import com.example.domain.model.AiResult
import com.example.domain.model.strategic.ComparisonResult
import com.example.domain.repository.AiRepository

class AnalyzeRealReplyUseCase(
  private val aiRepository: AiRepository
) {
  suspend operator fun invoke(
    situation: String,
    chosenOption: String,
    realReceivedReply: String
  ): AiResult<ComparisonResult> {
    if (realReceivedReply.isBlank()) {
      return AiResult.Error(AiError.Unknown("Veuillez saisir la réponse reçue."))
    }
    return aiRepository.analyzeRealReply(
      situation = situation,
      chosenOption = chosenOption,
      realReceivedReply = realReceivedReply.trim()
    )
  }
}
