package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ThemeMode {
  SYSTEM,
  DARK,
  LIGHT;

  fun next(): ThemeMode = when (this) {
    SYSTEM -> DARK
    DARK -> LIGHT
    LIGHT -> SYSTEM
  }
}

val LocalThemeIsDark = compositionLocalOf { true }

private val DarkColorScheme = darkColorScheme(
  primary = ElectricBlue,
  onPrimary = PureWhite,
  primaryContainer = DeepBlueSurfaceElevated,
  onPrimaryContainer = LuminousCyan,
  secondary = MysticViolet,
  onSecondary = PureWhite,
  secondaryContainer = DeepBlueSurface,
  onSecondaryContainer = PureWhite,
  tertiary = LuminousCyan,
  onTertiary = DeepBlue,
  background = DeepBlue,
  onBackground = TextPrimaryDark,
  surface = DeepBlueSurface,
  onSurface = TextPrimaryDark,
  surfaceVariant = DeepBlueSurfaceElevated,
  onSurfaceVariant = TextSecondaryDark,
  outline = GlassBorderSubtleDark,
  outlineVariant = GlassBorderDark
)

private val LightColorScheme = lightColorScheme(
  primary = ElectricBlue,
  onPrimary = PureWhite,
  primaryContainer = LightSurfaceElevated,
  onPrimaryContainer = DeepBlue,
  secondary = MysticViolet,
  onSecondary = PureWhite,
  secondaryContainer = LightSurfaceElevated,
  onSecondaryContainer = DeepBlue,
  tertiary = LuminousCyan,
  onTertiary = DeepBlue,
  background = LightBackground,
  onBackground = TextPrimaryLight,
  surface = LightSurface,
  onSurface = TextPrimaryLight,
  surfaceVariant = LightSurfaceElevated,
  onSurfaceVariant = TextSecondaryLight,
  outline = LightSurfaceBorder,
  outlineVariant = GlassBorderLight
)

@Composable
fun RizzAiTheme(
  themeMode: ThemeMode = ThemeMode.SYSTEM,
  content: @Composable () -> Unit,
) {
  val systemIsDark = isSystemInDarkTheme()
  val isDark = when (themeMode) {
    ThemeMode.SYSTEM -> systemIsDark
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
  }

  val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

  CompositionLocalProvider(LocalThemeIsDark provides isDark) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      content = content
    )
  }
}
