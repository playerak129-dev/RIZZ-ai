package com.example.domain.model

enum class ConversationStyle(
  val displayName: String,
  val emoji: String,
  val tagLine: String
) {
  NEUTRE("Neutre", "⚡", "Équilibré & spontané"),
  HUMOUR("Humour", "🎭", "Drôle, taquin & créatif"),
  CHARO("Charo", "🔥", "Séducteur, confiant & direct"),
  ROMANTIQUE("Romantique", "💖", "Tendre, sincère & attentionné"),
  CALCULATEUR("Calculateur", "🧠", "Stratégique, fin & analytique")
}
