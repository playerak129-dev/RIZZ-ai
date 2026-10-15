package com.example.domain.model

import java.util.UUID

data class Conversation(
  val id: String = UUID.randomUUID().toString(),
  val title: String = "Session RIZZ AI",
  val messages: List<Message> = emptyList(),
  val createdAt: Long = System.currentTimeMillis(),
)
