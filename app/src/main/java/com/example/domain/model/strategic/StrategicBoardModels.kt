package com.example.domain.model.strategic

data class StrategicBranch(
  val trigger: String,
  val hypothesis: String,
  val counterReply: String
)

data class StrategicOption(
  val id: String,
  val name: String,
  val proposedReply: String,
  val tone: String,
  val score: Int,
  val isRecommended: Boolean,
  val strengths: List<String>,
  val risks: List<String>,
  val branches: List<StrategicBranch>
)

data class StrategicBoardResult(
  val situation: String,
  val analysis: String,
  val coachAdvice: String,
  val options: List<StrategicOption>,
  val disclaimer: String = "Ces anticipations sont des hypothèses probabilistes de dynamique conversationnelle. Elles ne prétendent pas connaître avec certitude les pensées ou intentions d'une autre personne."
)

data class ComparisonResult(
  val receivedReply: String,
  val matchedBranch: String?,
  val interpretation: String,
  val recommendedNextMove: String,
  val coachInsight: String
)
