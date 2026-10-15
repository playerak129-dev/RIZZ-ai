package com.example.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.SecurityUtils
import com.example.domain.model.ConversationStyle
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "rizz_ai_preferences")

class AppPreferencesRepository(private val context: Context) {

  private object Keys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val LANGUAGE = stringPreferencesKey("app_language")
    val DEFAULT_STYLE = stringPreferencesKey("default_style")
    val DEFAULT_INTENSITY = intPreferencesKey("default_intensity")
    val PIN_LOCK_ENABLED = booleanPreferencesKey("pin_lock_enabled")
    val PIN_HASH = stringPreferencesKey("pin_hash")
  }

  val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
    val modeName = preferences[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
    try {
      ThemeMode.valueOf(modeName)
    } catch (e: Exception) {
      ThemeMode.SYSTEM
    }
  }

  val languageFlow: Flow<String> = context.dataStore.data.map { preferences ->
    preferences[Keys.LANGUAGE] ?: "fr"
  }

  val defaultStyleFlow: Flow<ConversationStyle> = context.dataStore.data.map { preferences ->
    val styleName = preferences[Keys.DEFAULT_STYLE] ?: ConversationStyle.NEUTRE.name
    try {
      ConversationStyle.valueOf(styleName)
    } catch (e: Exception) {
      ConversationStyle.NEUTRE
    }
  }

  val defaultIntensityFlow: Flow<Int> = context.dataStore.data.map { preferences ->
    preferences[Keys.DEFAULT_INTENSITY] ?: 3
  }

  val isPinLockEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
    preferences[Keys.PIN_LOCK_ENABLED] ?: false
  }

  val pinHashFlow: Flow<String?> = context.dataStore.data.map { preferences ->
    preferences[Keys.PIN_HASH]
  }

  suspend fun setThemeMode(mode: ThemeMode) {
    context.dataStore.edit { preferences ->
      preferences[Keys.THEME_MODE] = mode.name
    }
  }

  suspend fun setLanguage(languageCode: String) {
    context.dataStore.edit { preferences ->
      preferences[Keys.LANGUAGE] = languageCode
    }
  }

  suspend fun setDefaultStyle(style: ConversationStyle) {
    context.dataStore.edit { preferences ->
      preferences[Keys.DEFAULT_STYLE] = style.name
    }
  }

  suspend fun setDefaultIntensity(intensity: Int) {
    context.dataStore.edit { preferences ->
      preferences[Keys.DEFAULT_INTENSITY] = intensity.coerceIn(1, 5)
    }
  }

  suspend fun setPinLock(enabled: Boolean, rawPin: String? = null) {
    context.dataStore.edit { preferences ->
      preferences[Keys.PIN_LOCK_ENABLED] = enabled
      if (rawPin != null && rawPin.length == 4) {
        preferences[Keys.PIN_HASH] = SecurityUtils.hashPin(rawPin)
      } else if (!enabled) {
        preferences.remove(Keys.PIN_HASH)
      }
    }
  }

  suspend fun clearAllPreferences() {
    context.dataStore.edit { preferences ->
      preferences.clear()
    }
  }
}
