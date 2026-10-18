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
    level = HttpLoggingInterceptor.Level.BASIC
    redactHeader("x-goog-api-key")
    redactHeader("Authorization")
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
    val maxRetries = 2
    var attempt = 0
    var activeModel = AiConfig.MODEL_NAME

    while (true) {
      try {
        val response: GeminiResponseDto = service.generateContent(
          model = activeModel,
          apiKey = apiKey,
          request = request
        )

        val candidate = response.candidates?.firstOrNull()
        val responseText = candidate
          ?.content
          ?.parts
          ?.firstOrNull()
          ?.text
          ?.trim()

        if (!responseText.isNullOrBlank()) {
          return AiResult.Success(responseText)
        } else {
          val finishReason = candidate?.finishReason
          if (finishReason != null && finishReason != "STOP") {
            return when (finishReason) {
              "SAFETY" -> AiResult.Error(AiError.Unknown("La réponse a été filtrée par les règles de sécurité."))
              "RECITATION" -> AiResult.Error(AiError.Unknown("La réponse a été bloquée pour conformité."))
              else -> AiResult.Error(AiError.Unknown("Génération interrompue ($finishReason)."))
            }
          }
          val errorMessage = response.error?.message
          return if (errorMessage != null) {
            AiResult.Error(AiError.Unknown(errorMessage))
          } else {
            AiResult.Error(AiError.InvalidResponse)
          }
        }
      } catch (e: SocketTimeoutException) {
        if (attempt < maxRetries) {
          attempt++
          kotlinx.coroutines.delay(1000L * attempt)
          continue
        }
        return AiResult.Error(AiError.NetworkError)
      } catch (e: IOException) {
        if (attempt < maxRetries) {
          attempt++
          kotlinx.coroutines.delay(1000L * attempt)
          continue
        }
        return AiResult.Error(AiError.NetworkError)
      } catch (e: HttpException) {
        val code = e.code()
        // Transient server errors (500, 502, 503, 504) are eligible for limited retries
        if ((code == 500 || code == 502 || code == 503 || code == 504) && attempt < maxRetries) {
          attempt++
          kotlinx.coroutines.delay(1000L * attempt)
          continue
        }

        // Automatic fallback if model is not available or unrecognized
        if (code == 404 && activeModel != AiConfig.FALLBACK_MODEL_NAME) {
          activeModel = AiConfig.FALLBACK_MODEL_NAME
          continue
        }

        return when (code) {
          400 -> AiResult.Error(AiError.BadRequest)
          401 -> AiResult.Error(AiError.ApiKeyInvalid)
          403 -> AiResult.Error(AiError.PermissionDenied)
          404 -> AiResult.Error(AiError.ModelNotFound)
          429 -> AiResult.Error(AiError.QuotaExceeded)
          500, 502, 503, 504 -> AiResult.Error(AiError.ServerUnavailable)
          else -> AiResult.Error(AiError.Unknown("Erreur HTTP $code : ${e.message()}"))
        }
      } catch (e: Exception) {
        return AiResult.Error(AiError.Unknown(e.message ?: "Erreur inconnue"))
      }
    }
  }
}
