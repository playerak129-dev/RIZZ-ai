package com.example.domain.model

data class ConversationSummary(
  val id: String,
  val title: String,
  val lastMessageSnippet: String,
  val messageCount: Int,
  val activeStyle: ConversationStyle,
  val updatedAt: Long
)
