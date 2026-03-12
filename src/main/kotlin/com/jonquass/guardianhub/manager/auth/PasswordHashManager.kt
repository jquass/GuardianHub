package com.jonquass.guardianhub.manager.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.jonquass.guardianhub.config.Loggable
import com.jonquass.guardianhub.core.Result

object PasswordHashManager : Loggable {
  private val logger = logger()

  fun verifyHash(
      plainText: String,
      hash: String,
  ): Result<Unit> {
    try {
      val result = BCrypt.verifyer().verify(plainText.toCharArray(), hash.toCharArray())
      if (result.verified) {
        return Result.success()
      }
    } catch (e: Exception) {
      logger.error("Failed to verify hash: {}", e.message, e)
    }
    return Result.error()
  }

  fun hashPasswordResult(plainText: String): Result<String> {
    try {
      return Result.success(BCrypt.withDefaults().hashToString(10, plainText.toCharArray()))
    } catch (e: Exception) {
      logger.error("Failed to hash password: {}", e.message, e)
    }
    return Result.error()
  }
}
