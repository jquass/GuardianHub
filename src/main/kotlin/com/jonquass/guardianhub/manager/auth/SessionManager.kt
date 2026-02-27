package com.jonquass.guardianhub.manager.auth

import com.jonquass.guardianhub.config.Loggable
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object SessionManager : Loggable {
  private val logger = logger()
  private val sessions = ConcurrentHashMap<String, Long>()
  private const val SESSION_DURATION = 24 * 60 * 60 * 1000L // 24 hours

  fun createSession(): String {
    val token = UUID.randomUUID().toString()
    val expiresAt = System.currentTimeMillis() + SESSION_DURATION
    sessions[token] = expiresAt
    sessions.entries.removeIf { it.value < System.currentTimeMillis() }
    logger.info("Session started at {}", expiresAt)
    return token
  }

  fun isValidSession(token: String): Boolean {
    val expiresAt = sessions[token] ?: return false

    if (expiresAt < System.currentTimeMillis()) {
      sessions.remove(token)
      return false
    }

    return true
  }

  fun invalidateSession(token: String) {
    sessions.remove(token)
    logger.info("Session expired")
  }

  fun invalidateSessions() {
    sessions.clear()
    logger.info("All sessions have been cleared")
  }
}
