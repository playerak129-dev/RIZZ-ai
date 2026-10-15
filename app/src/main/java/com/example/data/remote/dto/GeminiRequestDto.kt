package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeminiRequestDto(
  @Json(name = "contents") val contents: List<ContentDto>,
  @Json(name = "systemInstruction") val systemInstruction: ContentDto? = null,
  @Json(name = "generationConfig") val generationConfig: GenerationConfigDto? = null
)

@JsonClass(generateAdapter = true)
data class ContentDto(
  @Json(name = "role") val role: String? = null,
  @Json(name = "parts") val parts: List<PartDto>
)

@JsonClass(generateAdapter = true)
data class PartDto(
  @Json(name = "text") val text: String? = null,
  @Json(name = "inlineData") val inlineData: InlineDataDto? = null
)

@JsonClass(generateAdapter = true)
data class InlineDataDto(
  @Json(name = "mimeType") val mimeType: String,
  @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfigDto(
  @Json(name = "temperature") val temperature: Float? = null,
  @Json(name = "topP") val topP: Float? = null
)
