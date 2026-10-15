package com.example.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
  @PrimaryKey
  val id: String,
  val title: String,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val activeStyle: String = "NEUTRE",
  val intensity: Int = 3,
  val isBlendModeEnabled: Boolean = false,
  val blendWeightsJson: String = ""
)
