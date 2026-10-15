package com.example.core

import java.security.MessageDigest

object SecurityUtils {
  private const val PIN_SALT = "RIZZ_AI_C4RL3NTZ_EDITION_LOCAL_SALT_2026"

  /**
   * Hashes the 4-digit PIN using SHA-256 and a constant salt.
   * Never stores plain PIN in preferences or memory.
   */
  fun hashPin(pin: String): String {
    val input = "$PIN_SALT:$pin"
    val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
  }

  fun verifyPin(inputPin: String, storedHash: String?): Boolean {
    if (storedHash.isNullOrBlank()) return false
    return hashPin(inputPin) == storedHash
  }
}
