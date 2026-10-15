package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiResponseDto(
  @Json(name = "candidates") val candidates: List<CandidateDto>? = null,
  @Json(name = "error") val error: GeminiApiErrorDto? = null
)

@JsonClass(generateAdapter = true)
data class CandidateDto(
  @Json(name = "content") val content: ContentDto? = null,
  @Json(name = "finishReason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiApiErrorDto(
  @Json(name = "code") val code: Int? = null,
  @Json(name = "message") val message: String? = null,
  @Json(name = "status") val status: String? = null
)
