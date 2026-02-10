package com.jonquass.guardianhub.managers

import at.favre.lib.crypto.bcrypt.BCrypt
import com.jonquass.guardianhub.config.Loggable
import com.jonquass.guardianhub.core.config.Env
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object AuthManager : Loggable {
    private val logger = logger()
    private val factoryPasswordFile = File("/opt/pi-stack/.factory-password")
    private val serialNumberFile = File("/opt/pi-stack/.serial-number")
    val sessions = ConcurrentHashMap<String, Long>()
    private const val SESSION_DURATION = 24 * 60 * 60 * 1000L // 24 hours

    fun validatePassword(password: String): Boolean {
        val loginPasswordHash = ConfigManager.getRawConfigValue(Env.LOGIN_PASSWORD)
        if (loginPasswordHash.isNullOrEmpty()) {
            logger.warn("Login password hash not found in .env")
            return false
        }

        return verifyHash(password, loginPasswordHash)
    }

    fun validateFactoryPassword(password: String): Boolean {
        if (!factoryPasswordFile.exists()) {
            logger.warn("Factory password file not found")
            return false
        }

        val factoryPasswordHash = factoryPasswordFile.readText().trim()
        return verifyHash(password, factoryPasswordHash)
    }

    fun validateSerialNumber(serial: String): Boolean {
        if (!serialNumberFile.exists()) {
            logger.warn("Serial number file not found")
            return false
        }

        val serialHash = serialNumberFile.readText().trim()
        return verifyHash(serial, serialHash)
    }

    fun resetToFactoryPassword(
        factoryPassword: String,
        serialNumber: String,
    ): Boolean {
        return try {
            // Validate factory password
            if (!validateFactoryPassword(factoryPassword)) {
                logger.error("Invalid factory password provided for reset")
                return false
            }

            // Validate serial number (physical access required)
            if (!validateSerialNumber(serialNumber)) {
                logger.error("Invalid serial number provided for reset")
                return false
            }

            // Read factory password hash
            val factoryHash = factoryPasswordFile.readText().trim()

            // Update .env with factory password hash
            ConfigManager.upsertConfig(Env.LOGIN_PASSWORD, factoryHash)

            // Invalidate all sessions
            sessions.clear()

            logger.info("Login password reset to factory password")
            true
        } catch (e: Exception) {
            logger.error("Failed to reset password: {}", e.message, e)
            false
        }
    }

    fun changeLoginPassword(
        currentPassword: String,
        newPassword: String,
        serialNumber: String,
    ): Boolean {
        return try {
            if (!validatePassword(currentPassword)) {
                logger.error("Current password is incorrect")
                return false
            }

            if (!validateSerialNumber(serialNumber)) {
                logger.error("Invalid serial number provided")
                return false
            }

            if (newPassword.length < 8) {
                logger.error("New password must be at least 8 characters (got {})", newPassword.length)
                return false
            }

            val newPasswordHash = hashPassword(newPassword)

            ConfigManager.upsertConfig(Env.LOGIN_PASSWORD, newPasswordHash)

            logger.info("Login password changed successfully")
            true
        } catch (e: Exception) {
            logger.error("Failed to change password: {}", e.message, e)
            false
        }
    }

    fun createSession(): String {
        val token = UUID.randomUUID().toString()
        val expiresAt = System.currentTimeMillis() + SESSION_DURATION
        sessions[token] = expiresAt

        sessions.entries.removeIf { it.value < System.currentTimeMillis() }

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
    }

    /**
     * Verify a password against a bcrypt hash
     * Supports $2a$, $2b$, $2x$, and $2y$ formats
     */
    private fun verifyHash(
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
