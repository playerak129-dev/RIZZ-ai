package com.example.presentation.components.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GlassBorderDark
import com.example.ui.theme.GlassBorderLight
import com.example.ui.theme.GlassBorderSubtleDark
import com.example.ui.theme.GlassBorderSubtleLight
import com.example.ui.theme.GlassSurfaceDark
import com.example.ui.theme.GlassSurfaceDarkElevated
import com.example.ui.theme.GlassSurfaceLight
import com.example.ui.theme.GlassSurfaceLightElevated
import com.example.ui.theme.LocalThemeIsDark
import com.example.ui.theme.LuminousCyan

/**
 * High-performance Liquid Glass surface styling for C4RL3NTZ Edition.
 * Combines translucent surface tinting, multi-stop specular borders, and subtle top-down light sheen
 * without requiring heavy GPU blur shaders on low-end hardware.
 */
@Composable
fun Modifier.liquidGlass(
  shape: Shape = RoundedCornerShape(20.dp),
  isElevated: Boolean = false,
  borderWidth: Dp = 1.dp,
  specularSheen: Boolean = true
): Modifier {
  val isDark = LocalThemeIsDark.current

  val surfaceGradient = if (isDark) {
    if (isElevated) {
      Brush.verticalGradient(
        listOf(
          GlassSurfaceDarkElevated,
          GlassSurfaceDark
        )
      )
    } else {
      Brush.verticalGradient(
        listOf(
          GlassSurfaceDark,
          GlassSurfaceDark.copy(alpha = 0.55f)
        )
      )
    }
  } else {
    if (isElevated) {
      Brush.verticalGradient(
        listOf(
          GlassSurfaceLightElevated,
          GlassSurfaceLight
        )
      )
    } else {
      Brush.verticalGradient(
        listOf(
          GlassSurfaceLight,
          GlassSurfaceLight.copy(alpha = 0.65f)
        )
      )
    }
  }

  val borderBrush = if (isDark) {
    Brush.linearGradient(
      colors = listOf(
        LuminousCyan.copy(alpha = 0.45f),
        GlassBorderDark,
        GlassBorderSubtleDark
      ),
      start = Offset(0f, 0f),
      end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )
  } else {
    Brush.linearGradient(
      colors = listOf(
        ElectricBlue.copy(alpha = 0.35f),
        GlassBorderLight,
        GlassBorderSubtleLight
      ),
      start = Offset(0f, 0f),
      end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )
  }

  val sheenColor = if (isDark) {
    Color.White.copy(alpha = 0.06f)
  } else {
    Color.White.copy(alpha = 0.4f)
  }

  return this
    .clip(shape)
    .background(surfaceGradient)
    .then(
      if (specularSheen) {
        Modifier.drawBehind {
          // Draw subtle upper specular sheen arc
          drawRect(
            brush = Brush.verticalGradient(
              colors = listOf(sheenColor, Color.Transparent),
              startY = 0f,
              endY = size.height * 0.45f
            )
          )
        }
      } else Modifier
    )
    .border(borderWidth, borderBrush, shape)
}

@Composable
fun LiquidGlassBox(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(20.dp),
  isElevated: Boolean = false,
  borderWidth: Dp = 1.dp,
  specularSheen: Boolean = true,
  content: @Composable BoxScope.() -> Unit
) {
  Box(
    modifier = modifier.liquidGlass(
      shape = shape,
      isElevated = isElevated,
      borderWidth = borderWidth,
      specularSheen = specularSheen
    ),
    content = content
  )
}
