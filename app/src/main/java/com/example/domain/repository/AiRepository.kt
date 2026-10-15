package com.example.domain.repository

import com.example.domain.model.AiResult
import com.example.domain.model.AttachedImage
import com.example.domain.model.Conversation
import com.example.domain.model.StyleSettings
import com.example.domain.model.strategic.ComparisonResult
import com.example.domain.model.strategic.StrategicBoardResult

interface AiRepository {
  suspend fun generateResponse(
    conversation: Conversation,
    userPrompt: String,
    attachedImage: AttachedImage?,
    styleSettings: StyleSettings = StyleSettings()
  ): AiResult<String>

  suspend fun generateRizzRapide(
    situation: String,
    styleSettings: StyleSettings = StyleSettings()
  ): AiResult<List<String>>

  suspend fun generateStrategicBoard(
    situation: String,
    styleSettings: StyleSettings = StyleSettings()
  ): AiResult<StrategicBoardResult>

  suspend fun analyzeRealReply(
    situation: String,
    chosenOption: String,
    realReceivedReply: String
  ): AiResult<ComparisonResult>
}
