package com.example.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.presentation.components.glass.LiquidGlassBox
import com.example.presentation.components.orb.OrbState
import com.example.presentation.components.orb.RizzAiOrb

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConversationEmptyState(
  orbState: OrbState,
  onSuggestionClick: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val suggestions = listOf(
    stringResource(R.string.suggestion_opener_1),
    stringResource(R.string.suggestion_opener_2),
    stringResource(R.string.suggestion_opener_3)
  )

  BoxWithConstraints(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    val totalHeight = maxHeight
    val totalWidth = maxWidth
    val isLandscapeOrCompactHeight = totalHeight < 480.dp || (totalWidth > totalHeight && totalWidth > 600.dp)

    if (isLandscapeOrCompactHeight) {
      // Landscape / Compact responsive layout (side-by-side)
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 24.dp, vertical = 12.dp)
          .verticalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
      ) {
        RizzAiOrb(
          state = orbState,
          diameter = if (totalHeight < 360.dp) 100.dp else 125.dp
        )

        Spacer(modifier = Modifier.width(24.dp))

        Column(
          modifier = Modifier.weight(1f, fill = false),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = stringResource(R.string.empty_state_title),
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.4.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = stringResource(R.string.empty_state_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(12.dp))

          FlowRow(
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            suggestions.forEachIndexed { index, suggestion ->
              SuggestionPill(
                text = suggestion,
                onClick = { onSuggestionClick(suggestion) },
                testTag = "suggestion_chip_$index"
              )
            }
          }
        }
      }
    } else {
      // Portrait / Regular height layout
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 24.dp, vertical = 20.dp)
          .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        // Living AI Orb
        val orbDiameter = if (totalHeight < 640.dp) 120.dp else 160.dp
        RizzAiOrb(
          state = orbState,
          diameter = orbDiameter
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
          text = stringResource(R.string.empty_state_title),
          style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
          ),
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = stringResource(R.string.empty_state_subtitle),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Interactive Liquid Glass suggestion chips
        FlowRow(
          horizontalArrangement = Arrangement.Center,
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          suggestions.forEachIndexed { index, suggestion ->
            SuggestionPill(
              text = suggestion,
              onClick = { onSuggestionClick(suggestion) },
              testTag = "suggestion_chip_$index"
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SuggestionPill(
  text: String,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier
) {
  LiquidGlassBox(
    shape = RoundedCornerShape(20.dp),
    isElevated = false,
    borderWidth = 1.dp,
    modifier = modifier
      .clickable(onClick = onClick)
      .testTag(testTag)
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.bodySmall.copy(
        fontWeight = FontWeight.Medium
      ),
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
    )
  }
}
