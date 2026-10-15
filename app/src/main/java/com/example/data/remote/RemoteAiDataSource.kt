package com.example.data.remote

import com.example.core.AiConfig
import com.example.data.remote.dto.GeminiRequestDto
import com.example.data.remote.dto.GeminiResponseDto
import com.example.domain.model.AiError
import com.example.domain.model.AiResult
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class RemoteAiDataSource(
  private val apiService: GeminiApiService? = null
) {
  private val moshi: Moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

  private val loggingInterceptor = HttpLoggingInterceptor().apply {
    // Basic level: logs method and URL response status only, never logging sensitive headers, API keys or image bytes
    level = HttpLoggingInterceptor.Level.BASIC
  }

  private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(AiConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .readTimeout(AiConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .writeTimeout(AiConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .addInterceptor(loggingInterceptor)
    .build()

  private val service: GeminiApiService by lazy {
    apiService ?: Retrofit.Builder()
      .baseUrl(AiConfig.BASE_URL)
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(GeminiApiService::class.java)
  }

  suspend fun generateContent(apiKey: String, request: GeminiRequestDto): AiResult<String> {
    return try {
      val response: GeminiResponseDto = service.generateContent(
        model = AiConfig.MODEL_NAME,
        apiKey = apiKey,
        request = request
      )

      val responseText = response.candidates
        ?.firstOrNull()
        ?.content
        ?.parts
        ?.firstOrNull()
        ?.text
        ?.trim()

      if (!responseText.isNullOrBlank()) {
        AiResult.Success(responseText)
      } else {
        val errorMessage = response.error?.message
        if (errorMessage != null) {
          AiResult.Error(AiError.Unknown(errorMessage))
        } else {
          AiResult.Error(AiError.InvalidResponse)
        }
      }
    } catch (e: SocketTimeoutException) {
      AiResult.Error(AiError.NetworkError)
    } catch (e: IOException) {
      AiResult.Error(AiError.NetworkError)
    } catch (e: HttpException) {
      when (e.code()) {
        400 -> AiResult.Error(AiError.Unknown("Requête invalide ou paramètre non pris en charge."))
        401, 403 -> AiResult.Error(AiError.ApiKeyMissing)
        429 -> AiResult.Error(AiError.QuotaExceeded)
        500, 503 -> AiResult.Error(AiError.Unknown("Le service Gemini est temporairement indisponible. Réessayez."))
        else -> AiResult.Error(AiError.Unknown("Erreur HTTP ${e.code()} : ${e.message()}"))
      }
    } catch (e: Exception) {
      AiResult.Error(AiError.Unknown(e.message ?: "Erreur inconnue"))
    }
  }
}
