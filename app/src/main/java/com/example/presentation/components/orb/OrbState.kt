package com.example.presentation.components.orb

/**
 * Visual states of the living RIZZ AI Orb.
 * During Phase 2, visual animations are established.
 * Interactive state (FOCUSED) responds to user input, while THINKING and STREAMING
 * are prepared for Phase 3 real AI logic integration.
 */
enum class OrbState {
  IDLE,
  FOCUSED,
  THINKING,
  STREAMING,
  SUCCESS,
  ERROR
}
