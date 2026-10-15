package com.example.core

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateTimeUtils {
  private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

  fun formatTime(timestamp: Long): String {
    return timeFormat.format(Date(timestamp))
  }
}
