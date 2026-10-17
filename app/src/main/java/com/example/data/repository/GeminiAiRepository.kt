package com.example.data.repository

import com.example.BuildConfig
import com.example.core.AiConfig
import com.example.core.MarkdownFormatter
import com.example.data.remote.RemoteAiDataSource
import com.example.data.remote.dto.ContentDto
import com.example.data.remote.dto.GeminiRequestDto
import com.example.data.remote.dto.GenerationConfigDto
import com.example.data.remote.dto.InlineDataDto
import com.example.data.remote.dto.PartDto
import com.example.domain.model.AiError
import com.example.domain.model.AiResult
import com.example.domain.model.AttachedImage
import com.example.domain.model.Conversation
import com.example.domain.model.MessageRole
import com.example.domain.model.StyleSettings
import com.example.domain.model.strategic.ComparisonResult
import com.example.domain.model.strategic.StrategicBoardResult
import com.example.domain.model.strategic.StrategicBranch
import com.example.domain.model.strategic.StrategicOption
import com.example.domain.repository.AiRepository
import org.json.JSONObject

class GeminiAiRepository(
  private val remoteDataSource: RemoteAiDataSource = RemoteAiDataSource(),
  private val apiKeyProvider: () -> String = { BuildConfig.GEMINI_API_KEY }
) : AiRepository {

  override suspend fun generateResponse(
    conversation: Conversation,
    userPrompt: String,
    attachedImage: AttachedImage?,
    styleSettings: StyleSettings
  ): AiResult<String> {
    val apiKey = apiKeyProvider().trim()

    if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
      return AiResult.Error(AiError.ApiKeyMissing)
    }

    val historyMessages = conversation.messages
      .filter { it.role == MessageRole.USER || it.role == MessageRole.ASSISTANT }
      .let { list ->
        if (list.isNotEmpty() && list.last().role == MessageRole.USER && list.last().content.trim() == userPrompt.trim()) {
          list.dropLast(1)
        } else {
          list
        }
      }
      .takeLast(AiConfig.MAX_CONTEXT_MESSAGES)

    val contents = mutableListOf<ContentDto>()

    for (msg in historyMessages) {
      val role = if (msg.role == MessageRole.USER) "user" else "model"
      if (msg.content.isNotBlank()) {
        if (contents.isNotEmpty() && contents.last().role == role) {
          val lastParts = contents.last().parts.toMutableList()
          lastParts.add(PartDto(text = msg.content))
          contents[contents.lastIndex] = contents.last().copy(parts = lastParts)
        } else {
          contents.add(ContentDto(role = role, parts = listOf(PartDto(text = msg.content))))
        }
      }
    }

    val currentParts = mutableListOf<PartDto>()
    if (attachedImage != null && attachedImage.base64Data.isNotBlank()) {
      currentParts.add(
        PartDto(
          inlineData = InlineDataDto(
            mimeType = attachedImage.mimeType,
            data = attachedImage.base64Data
          )
        )
      )
    }

    if (userPrompt.isNotBlank()) {
      currentParts.add(PartDto(text = userPrompt))
    }

    if (currentParts.isEmpty()) {
      return AiResult.Error(AiError.Unknown("Aucun contenu ni image à envoyer."))
    }

    contents.add(ContentDto(role = "user", parts = currentParts))

    val systemPrompt = AiConfig.buildSystemInstruction(styleSettings)
    val calculatedTemp = (0.65f + (styleSettings.intensity * 0.06f)).coerceIn(0.6f, 1.0f)

    val request = GeminiRequestDto(
      contents = contents,
      systemInstruction = ContentDto(
        parts = listOf(PartDto(text = systemPrompt))
      ),
      generationConfig = GenerationConfigDto(
        temperature = calculatedTemp,
        topP = 0.95f
      )
    )

    return remoteDataSource.generateContent(apiKey = apiKey, request = request)
  }

  override suspend fun generateRizzRapide(
    situation: String,
    styleSettings: StyleSettings
  ): AiResult<List<String>> {
    val apiKey = apiKeyProvider().trim()

    if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
      return AiResult.Error(AiError.ApiKeyMissing)
    }

    val systemPrompt = AiConfig.buildRizzRapideSystemInstruction(styleSettings)
    val userTurn = ContentDto(
      role = "user",
      parts = listOf(PartDto(text = "Situation ou message reçu : $situation"))
    )

    val request = GeminiRequestDto(
      contents = listOf(userTurn),
      systemInstruction = ContentDto(
        parts = listOf(PartDto(text = systemPrompt))
      ),
      generationConfig = GenerationConfigDto(
        temperature = 0.85f,
        topP = 0.95f
      )
    )

    val remoteResult = remoteDataSource.generateContent(apiKey = apiKey, request = request)
    return when (remoteResult) {
      is AiResult.Success -> {
        val parsedOptions = parseRizzRapideOptions(remoteResult.data)
        if (parsedOptions.isNotEmpty()) {
          AiResult.Success(parsedOptions)
        } else {
          AiResult.Success(listOf(remoteResult.data))
        }
      }
      is AiResult.Error -> AiResult.Error(remoteResult.error)
    }
  }

  override suspend fun generateStrategicBoard(
    situation: String,
    styleSettings: StyleSettings
  ): AiResult<StrategicBoardResult> {
    val apiKey = apiKeyProvider().trim()

    if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
      return AiResult.Error(AiError.ApiKeyMissing)
    }

    val systemPrompt = AiConfig.buildStrategicBoardSystemInstruction()
    val userTurn = ContentDto(
      role = "user",
      parts = listOf(
        PartDto(
          text = "Contexte / message de la cible à analyser sur l'échiquier : $situation"
        )
      )
    )

    val request = GeminiRequestDto(
      contents = listOf(userTurn),
      systemInstruction = ContentDto(
        parts = listOf(PartDto(text = systemPrompt))
      ),
      generationConfig = GenerationConfigDto(
        temperature = 0.7f,
        topP = 0.95f
      )
    )

    val remoteResult = remoteDataSource.generateContent(apiKey = apiKey, request = request)
    return when (remoteResult) {
      is AiResult.Success -> {
        try {
          val parsed = parseStrategicBoardResult(situation, remoteResult.data)
          AiResult.Success(parsed)
        } catch (e: Exception) {
          // If JSON extraction fails, provide formatted fallback result
          AiResult.Success(
            createFallbackStrategicResult(situation, remoteResult.data)
          )
        }
      }
      is AiResult.Error -> AiResult.Error(remoteResult.error)
    }
  }

  override suspend fun analyzeRealReply(
    situation: String,
    chosenOption: String,
    realReceivedReply: String
  ): AiResult<ComparisonResult> {
    val apiKey = apiKeyProvider().trim()

    if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
      return AiResult.Error(AiError.ApiKeyMissing)
    }

    val systemPrompt = AiConfig.buildComparisonSystemInstruction()
    val userTurn = ContentDto(
      role = "user",
      parts = listOf(
        PartDto(
          text = """
            Situation initiale : $situation
            Réponse envoyée : $chosenOption
            Réponse réelle reçue : $realReceivedReply
          """.trimIndent()
        )
      )
    )

    val request = GeminiRequestDto(
      contents = listOf(userTurn),
      systemInstruction = ContentDto(
        parts = listOf(PartDto(text = systemPrompt))
      ),
      generationConfig = GenerationConfigDto(
        temperature = 0.7f,
        topP = 0.95f
      )
    )

    val remoteResult = remoteDataSource.generateContent(apiKey = apiKey, request = request)
    return when (remoteResult) {
      is AiResult.Success -> {
        try {
          val parsed = parseComparisonResult(realReceivedReply, remoteResult.data)
          AiResult.Success(parsed)
        } catch (e: Exception) {
          AiResult.Success(
            ComparisonResult(
              receivedReply = realReceivedReply,
              matchedBranch = "Réaction observée",
              interpretation = remoteResult.data.take(150),
              recommendedNextMove = "Conserver un tempo mesuré et relancer avec naturel.",
              coachInsight = "L'authenticité et le détachement l'emportent toujours sur la précipitation."
            )
          )
        }
      }
      is AiResult.Error -> AiResult.Error(remoteResult.error)
    }
  }

  private fun parseStrategicBoardResult(situation: String, rawText: String): StrategicBoardResult {
    val cleanJson = extractJsonPayload(rawText)
    val root = JSONObject(cleanJson)

    val analysis = root.optString("analysis", "Analyse dynamique de l'échange.")
    val coachAdvice = root.optString("coachAdvice", "Garde le tempo et assume ton style.")

    val optionsList = mutableListOf<StrategicOption>()
    if (root.has("options")) {
      val optionsArray = root.getJSONArray("options")
      for (i in 0 until optionsArray.length()) {
        val optObj = optionsArray.getJSONObject(i)
        val id = optObj.optString("id", "opt_$i")
        val name = optObj.optString("name", "Option ${i + 1}")
        val proposedReply = cleanOptionText(optObj.optString("proposedReply", ""))
        val tone = optObj.optString("tone", "Équilibré")
        val score = optObj.optInt("score", 85)
        val isRecommended = optObj.optBoolean("isRecommended", i == 0)

        val strengths = mutableListOf<String>()
        if (optObj.has("strengths")) {
          val stArr = optObj.getJSONArray("strengths")
          for (s in 0 until stArr.length()) strengths.add(stArr.getString(s))
        }

        val risks = mutableListOf<String>()
        if (optObj.has("risks")) {
          val rkArr = optObj.getJSONArray("risks")
          for (r in 0 until rkArr.length()) risks.add(rkArr.getString(r))
        }

        val branches = mutableListOf<StrategicBranch>()
        if (optObj.has("branches")) {
          val brArr = optObj.getJSONArray("branches")
          for (b in 0 until brArr.length()) {
            val brObj = brArr.getJSONObject(b)
            branches.add(
              StrategicBranch(
                trigger = brObj.optString("trigger", "Réaction"),
                hypothesis = brObj.optString("hypothesis", "Hypothèse"),
                counterReply = cleanOptionText(brObj.optString("counterReply", ""))
              )
            )
          }
        }

        optionsList.add(
          StrategicOption(
            id = id,
            name = name,
            proposedReply = proposedReply,
            tone = tone,
            score = score,
            isRecommended = isRecommended,
            strengths = strengths,
            risks = risks,
            branches = branches
          )
        )
      }
    }

    return StrategicBoardResult(
      situation = situation,
      analysis = analysis,
      coachAdvice = coachAdvice,
      options = optionsList
    )
  }

  private fun parseComparisonResult(receivedReply: String, rawText: String): ComparisonResult {
    val cleanJson = extractJsonPayload(rawText)
    val root = JSONObject(cleanJson)
    return ComparisonResult(
      receivedReply = receivedReply,
      matchedBranch = root.optString("matchedBranch", "Branche correspondante"),
      interpretation = root.optString("interpretation", "Analyse de la réaction"),
      recommendedNextMove = root.optString("recommendedNextMove", "Contre-réponse conseillée"),
      coachInsight = root.optString("coachInsight", "Conseil coach")
    )
  }

  private fun extractJsonPayload(text: String): String {
    val trimmed = text.trim()
    val jsonBlockRegex = Regex("```(?:json)?\\s*([\\s\\S]*?)\\s*```", RegexOption.IGNORE_CASE)
    val match = jsonBlockRegex.find(trimmed)
    if (match != null) {
      return match.groupValues[1].trim()
    }
    val firstBrace = trimmed.indexOf('{')
    val lastBrace = trimmed.lastIndexOf('}')
    if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
      return trimmed.substring(firstBrace, lastBrace + 1)
    }
    return trimmed
  }

  private fun createFallbackStrategicResult(situation: String, rawText: String): StrategicBoardResult {
    return StrategicBoardResult(
      situation = situation,
      analysis = "Analyse préliminaire de la situation soumise.",
      coachAdvice = "Privilégie le détachement amusé et l'écoute active.",
      options = listOf(
        StrategicOption(
          id = "fallback_opt_1",
          name = "Option Recommandée",
          proposedReply = cleanOptionText(rawText.lines().firstOrNull { it.isNotBlank() } ?: "Salut, sympa ton message."),
          tone = "Charo & Séduisant",
          score = 90,
          isRecommended = true,
          strengths = listOf("Direct", "Dynamique"),
          risks = listOf("Nécessite de l'assurance"),
          branches = listOf(
            StrategicBranch(
              trigger = "Si enthousiasme",
              hypothesis = "Hypothèse de complicité",
              counterReply = "Enchaîner sur une proposition concrète"
            ),
            StrategicBranch(
              trigger = "Si réserve",
              hypothesis = "Hypothèse de prudence",
              counterReply = "Dédramatiser avec humour"
            )
          )
        )
      )
    )
  }

  private fun parseRizzRapideOptions(rawText: String): List<String> {
    val lines = rawText.lines()
    val options = mutableListOf<String>()
    var currentOption = StringBuilder()

    for (line in lines) {
      val trimmed = line.trim()
      val isNewOption = trimmed.startsWith("OPTION 1:", ignoreCase = true) ||
          trimmed.startsWith("OPTION 2:", ignoreCase = true) ||
          trimmed.startsWith("OPTION 3:", ignoreCase = true) ||
          trimmed.startsWith("1.", ignoreCase = true) ||
          trimmed.startsWith("2.", ignoreCase = true) ||
          trimmed.startsWith("3.", ignoreCase = true)

      if (isNewOption) {
        if (currentOption.isNotBlank()) {
          options.add(cleanOptionText(currentOption.toString()))
          currentOption = StringBuilder()
        }
        val cleanedLine = trimmed
          .replace(Regex("^(OPTION \\d+:?|\\d+\\.)\\s*", RegexOption.IGNORE_CASE), "")
        currentOption.append(cleanedLine)
      } else if (trimmed.isNotBlank()) {
        if (currentOption.isNotEmpty()) currentOption.append(" ")
        currentOption.append(trimmed)
      }
    }

    if (currentOption.isNotBlank()) {
      options.add(cleanOptionText(currentOption.toString()))
    }

    return if (options.size >= 2) options.take(3) else listOf(cleanOptionText(rawText))
  }

  private fun cleanOptionText(text: String): String {
    val unquoted = text.trim().removeSurrounding("\"").removeSurrounding("«", "»").trim()
    return MarkdownFormatter.cleanForPlainText(unquoted)
  }
}
