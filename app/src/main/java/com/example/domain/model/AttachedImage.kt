package com.example.domain.model

data class AttachedImage(
  val uri: String,
  val base64Data: String,
  val mimeType: String = "image/jpeg"
)
