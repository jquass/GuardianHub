package com.jonquass.guardianhub.manager.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.jonquass.guardianhub.config.Loggable

object PasswordHashManager : Loggable {
    private val logger = logger()

    /**
     * Verify a password against a bcrypt hash
     * Supports $2a$, $2b$, $2x$, and $2y$ formats
     */
    fun verifyHash(
        plainText: String,
        hash: String,
    ): Boolean =
        try {
            val result = BCrypt.verifyer().verify(plainText.toCharArray(), hash.toCharArray())
            result.verified
        } catch (e: Exception) {
            logger.error("Failed to verify hash: {}", e.message, e)
            false
        }

    /**
     * Hash a password using bcrypt
     */
    fun hashPassword(plainText: String): String =
        try {
            BCrypt.withDefaults().hashToString(10, plainText.toCharArray())
        } catch (e: Exception) {
            logger.error("Failed to hash password: {}", e.message, e)
            throw e
        }
}
