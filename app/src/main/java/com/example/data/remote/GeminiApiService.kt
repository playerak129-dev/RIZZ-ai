package com.example.data.remote

import com.example.data.remote.dto.GeminiRequestDto
import com.example.data.remote.dto.GeminiResponseDto
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface GeminiApiService {
  @POST("v1beta/models/{model}:generateContent")
  suspend fun generateContent(
    @Path("model") model: String,
    @Header("x-goog-api-key") apiKey: String,
    @Body request: GeminiRequestDto
  ): GeminiResponseDto
}
