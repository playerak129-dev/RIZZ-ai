package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.emoji2.text.DefaultEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.presentation.conversation.ConversationScreen
import com.example.presentation.conversation.ConversationViewModel
import com.example.ui.theme.RizzAiTheme

class MainActivity : ComponentActivity() {
  val viewModel: ConversationViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    initEmojiCompat()
    enableEdgeToEdge()
    setContent {
      val uiState by viewModel.uiState.collectAsStateWithLifecycle()
      RizzAiTheme(themeMode = uiState.themeMode) {
        ConversationScreen(
          viewModel = viewModel,
          modifier = Modifier.fillMaxSize()
        )
      }
    }
  }

  private fun initEmojiCompat() {
    try {
      if (!EmojiCompat.isConfigured()) {
        val config = DefaultEmojiCompatConfig.create(this)
        if (config != null) {
          config.setReplaceAll(true)
          EmojiCompat.init(config)
        }
      }
    } catch (_: Throwable) {
      // Graceful fallback to system font in testing or offline environments
    }
  }
}
