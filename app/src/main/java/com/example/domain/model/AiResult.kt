package com.example.domain.model

sealed interface AiResult<out T> {
  data class Success<T>(val data: T) : AiResult<T>
  data class Error(val error: AiError) : AiResult<Nothing>
}

sealed class AiError(val message: String) {
  object ApiKeyMissing : AiError("Clé API Gemini non configurée dans AI Studio Secrets.")
  object NetworkError : AiError("Connexion réseau indisponible ou délai d'attente dépassé.")
  object QuotaExceeded : AiError("Quota d'appels API dépassé. Veuillez réessayer dans quelques instants.")
  object InvalidResponse : AiError("Le modèle distant a renvoyé une réponse inattendue ou vide.")
  object ImageTooLarge : AiError("L'image est trop volumineuse pour être traitée.")
  data class Unknown(val detail: String) : AiError("Erreur du service IA : $detail")
}
