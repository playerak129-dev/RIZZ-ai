package com.example.domain.model

sealed interface AiResult<out T> {
  data class Success<T>(val data: T) : AiResult<T>
  data class Error(val error: AiError) : AiResult<Nothing>
}

sealed class AiError(val message: String) {
  object ApiKeyMissing : AiError("Clé API Gemini non configurée dans AI Studio Secrets.")
  object ApiKeyInvalid : AiError("Clé API Gemini invalide ou expirée (erreur 401). Vérifiez votre clé dans les Secrets.")
  object PermissionDenied : AiError("Accès refusé (erreur 403). Vérifiez que l'API Generative Language est activée sur votre compte Google.")
  object QuotaExceeded : AiError("Quota d'appels API dépassé (erreur 429). Veuillez réessayer dans quelques instants.")
  object ServerUnavailable : AiError("Le service Gemini est temporairement indisponible (erreur 5xx). Réessayez dans quelques instants.")
  object BadRequest : AiError("Requête invalide ou paramètre non pris en charge (erreur 400).")
  object ModelNotFound : AiError("Modèle Gemini demandé introuvable ou non disponible (erreur 404).")
  object NetworkError : AiError("Connexion réseau indisponible ou délai d'attente dépassé.")
  object InvalidResponse : AiError("Le modèle distant a renvoyé une réponse inattendue ou vide.")
  object ImageTooLarge : AiError("L'image est trop volumineuse pour être traitée.")
  data class Unknown(val detail: String) : AiError("Erreur du service IA : $detail")
}
