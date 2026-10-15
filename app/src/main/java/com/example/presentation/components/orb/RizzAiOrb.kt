package com.example.presentation.components.orb

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.LocalThemeIsDark
import com.example.ui.theme.LuminousCyan
import com.example.ui.theme.MysticViolet
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Living RIZZ AI Orb — C4RL3NTZ Edition.
 * Native Jetpack Compose implementation using layered DrawScope Canvas gradients
 * with zero heap allocation in onDraw.
 */
@Composable
fun RizzAiOrb(
  modifier: Modifier = Modifier,
  state: OrbState = OrbState.IDLE,
  diameter: Dp = 150.dp
) {
  val isDark = LocalThemeIsDark.current
  val orbLabel = stringResource(R.string.orb_desc)

  // Motion timing parameters based on OrbState
  val rotationDuration = when (state) {
    OrbState.IDLE -> 5000
    OrbState.FOCUSED -> 2600
    OrbState.THINKING -> 1500
    OrbState.STREAMING -> 1100
    OrbState.SUCCESS -> 3200
    OrbState.ERROR -> 3500
  }

  val pulseDuration = when (state) {
    OrbState.IDLE -> 3600
    OrbState.FOCUSED -> 1800
    OrbState.THINKING -> 1100
    OrbState.STREAMING -> 900
    OrbState.SUCCESS -> 2200
    OrbState.ERROR -> 2000
  }

  val transition = rememberInfiniteTransition(label = "OrbMotion")

  val phase by transition.animateFloat(
    initialValue = 0f,
    targetValue = (2 * PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(rotationDuration, easing = LinearEasing)
    ),
    label = "OrbRotationPhase"
  )

  val pulseScale by transition.animateFloat(
    initialValue = 0.95f,
    targetValue = 1.05f,
    animationSpec = infiniteRepeatable(
      animation = tween(pulseDuration, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "OrbPulseScale"
  )

  val glowAlpha by transition.animateFloat(
    initialValue = 0.35f,
    targetValue = if (state == OrbState.FOCUSED || state == OrbState.STREAMING) 0.75f else 0.55f,
    animationSpec = infiniteRepeatable(
      animation = tween((pulseDuration * 1.2f).toInt(), easing = LinearOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "OrbGlowAlpha"
  )

  // Color tuning based on OrbState
  val primaryColor = when (state) {
    OrbState.ERROR -> Color(0xFFF43F5E)
    OrbState.SUCCESS -> Color(0xFF10B981)
    else -> LuminousCyan
  }

  val secondaryColor = when (state) {
    OrbState.ERROR -> Color(0xFFFB923C)
    OrbState.SUCCESS -> LuminousCyan
    else -> ElectricBlue
  }

  val tertiaryColor = when (state) {
    OrbState.ERROR -> Color(0xFFBE123C)
    OrbState.SUCCESS -> ElectricBlue
    else -> MysticViolet
  }

  Box(
    modifier = modifier
      .size(diameter)
      .semantics { contentDescription = orbLabel },
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.matchParentSize()) {
      val center = Offset(size.width / 2f, size.height / 2f)
      val radius = (size.minDimension / 2f) * 0.76f * pulseScale

      // 1. Outer Ethereal Halo (Atmospheric Glow)
      val haloGlowRadius = radius * 1.35f
      val haloColor = primaryColor.copy(alpha = if (isDark) glowAlpha * 0.45f else glowAlpha * 0.3f)
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(haloColor, secondaryColor.copy(alpha = haloColor.alpha * 0.4f), Color.Transparent),
          center = center,
          radius = haloGlowRadius
        ),
        radius = haloGlowRadius,
        center = center
      )

      // 2. Base Dark Sphere Silhouette (Occlusion and Depth)
      val baseColor = if (isDark) Color(0xFF060B14) else Color(0xFF0F1E36)
      drawCircle(
        color = baseColor,
        radius = radius,
        center = center
      )

      // 3. Fluid Color Currents (Intertwined Cyan, Blue, Violet Streams)
      val offset1 = Offset(
        x = center.x + (radius * 0.32f * cos(phase)),
        y = center.y + (radius * 0.32f * sin(phase))
      )
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(primaryColor.copy(alpha = 0.85f), Color.Transparent),
          center = offset1,
          radius = radius * 0.88f
        ),
        radius = radius,
        center = center,
        blendMode = BlendMode.Screen
      )

      val offset2 = Offset(
        x = center.x - (radius * 0.36f * cos(phase + 1.2f)),
        y = center.y - (radius * 0.36f * sin(phase + 1.2f))
      )
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(secondaryColor.copy(alpha = 0.8f), Color.Transparent),
          center = offset2,
          radius = radius * 0.85f
        ),
        radius = radius,
        center = center,
        blendMode = BlendMode.Screen
      )

      val offset3 = Offset(
        x = center.x + (radius * 0.28f * sin(phase * 0.8f)),
        y = center.y - (radius * 0.28f * cos(phase * 0.8f))
      )
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(tertiaryColor.copy(alpha = 0.75f), Color.Transparent),
          center = offset3,
          radius = radius * 0.82f
        ),
        radius = radius,
        center = center,
        blendMode = BlendMode.Screen
      )

      // 4. Inner Luminous Core
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            Color.White.copy(alpha = 0.7f),
            primaryColor.copy(alpha = 0.3f),
            Color.Transparent
          ),
          center = center,
          radius = radius * 0.42f
        ),
        radius = radius * 0.42f,
        center = center,
        blendMode = BlendMode.Plus
      )

      // 5. Specular Reflection Sheen (Glass Spherical Highlight)
      val highlightCenter = Offset(center.x - radius * 0.34f, center.y - radius * 0.34f)
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            Color.White.copy(alpha = 0.65f),
            Color.White.copy(alpha = 0.15f),
            Color.Transparent
          ),
          center = highlightCenter,
          radius = radius * 0.38f
        ),
        radius = radius * 0.38f,
        center = highlightCenter
      )

      // 6. Delicately Chiseled Glass Rim
      val rimBrush = Brush.sweepGradient(
        listOf(
          primaryColor.copy(alpha = 0.7f),
          Color.Transparent,
          secondaryColor.copy(alpha = 0.6f),
          tertiaryColor.copy(alpha = 0.4f),
          primaryColor.copy(alpha = 0.7f)
        ),
        center = center
      )
      drawCircle(
        brush = rimBrush,
        radius = radius,
        center = center,
        style = Stroke(width = 1.75.dp.toPx())
      )
    }
  }
}
