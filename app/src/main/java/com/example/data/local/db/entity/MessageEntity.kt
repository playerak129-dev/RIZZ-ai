package com.example.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "messages",
  foreignKeys = [
    ForeignKey(
      entity = ConversationEntity::class,
      parentColumns = ["id"],
      childColumns = ["conversationId"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [
    Index(value = ["conversationId"]),
    Index(value = ["timestamp"])
  ]
)
data class MessageEntity(
  @PrimaryKey
  val id: String,
  val conversationId: String,
  val role: String,
  val content: String,
  val timestamp: Long = System.currentTimeMillis(),
  val imageUri: String? = null
)
