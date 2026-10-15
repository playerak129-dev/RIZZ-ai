package com.example.domain.model

data class StyleSettings(
  val activeStyle: ConversationStyle = ConversationStyle.NEUTRE,
  val intensity: Int = 3,
  val isBlendModeEnabled: Boolean = false,
  val weights: Map<ConversationStyle, Int> = mapOf(
    ConversationStyle.CHARO to 40,
    ConversationStyle.HUMOUR to 30,
    ConversationStyle.ROMANTIQUE to 20,
    ConversationStyle.CALCULATEUR to 10
  )
) {
  init {
    require(intensity in 1..5) { "L'intensité doit être comprise entre 1 et 5." }
  }

  /**
   * Returns weights mathematically normalized so the sum is guaranteed to be exactly 100%.
   */
  fun getNormalizedWeights(): Map<ConversationStyle, Int> {
    val styles = listOf(
      ConversationStyle.CHARO,
      ConversationStyle.HUMOUR,
      ConversationStyle.ROMANTIQUE,
      ConversationStyle.CALCULATEUR
    )

    val currentSum = styles.sumOf { weights[it] ?: 0 }
    if (currentSum == 100) return weights.filterKeys { it in styles }

    if (currentSum == 0) {
      val split = 100 / styles.size
      val remainder = 100 % styles.size
      return styles.mapIndexed { index, style ->
        style to (split + if (index < remainder) 1 else 0)
      }.toMap()
    }

    var allocated = 0
    val normalized = mutableMapOf<ConversationStyle, Int>()
    styles.forEachIndexed { index, style ->
      if (index == styles.lastIndex) {
        normalized[style] = (100 - allocated).coerceAtLeast(0)
      } else {
        val share = (((weights[style] ?: 0).toFloat() / currentSum) * 100).toInt()
        normalized[style] = share
        allocated += share
      }
    }
    return normalized
  }

  /**
   * Adjusts one style's weight and reallocates the remainder proportionally across the other styles,
   * guaranteeing that the total remains strictly 100%.
   */
  fun withAdjustedWeight(targetStyle: ConversationStyle, newWeight: Int): StyleSettings {
    val clampedNew = newWeight.coerceIn(0, 100)
    val styles = listOf(
      ConversationStyle.CHARO,
      ConversationStyle.HUMOUR,
      ConversationStyle.ROMANTIQUE,
      ConversationStyle.CALCULATEUR
    )
    val others = styles.filter { it != targetStyle }

    val remaining = 100 - clampedNew
    val currentOthersSum = others.sumOf { weights[it] ?: 0 }

    val newMap = mutableMapOf<ConversationStyle, Int>()
    newMap[targetStyle] = clampedNew

    if (currentOthersSum == 0) {
      val split = remaining / others.size
      val rem = remaining % others.size
      others.forEachIndexed { index, st ->
        newMap[st] = split + if (index < rem) 1 else 0
      }
    } else {
      var allocated = 0
      others.forEachIndexed { index, st ->
        if (index == others.lastIndex) {
          newMap[st] = (remaining - allocated).coerceAtLeast(0)
        } else {
          val share = (((weights[st] ?: 0).toFloat() / currentOthersSum) * remaining).toInt()
          newMap[st] = share
          allocated += share
        }
      }
    }

    return copy(weights = newMap)
  }

  fun withResetWeights(): StyleSettings {
    return copy(
      weights = mapOf(
        ConversationStyle.CHARO to 40,
        ConversationStyle.HUMOUR to 30,
        ConversationStyle.ROMANTIQUE to 20,
        ConversationStyle.CALCULATEUR to 10
      )
    )
  }
}
