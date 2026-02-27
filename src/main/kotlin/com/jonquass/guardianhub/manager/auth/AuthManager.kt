package com.jonquass.guardianhub.manager.auth

import com.jonquass.guardianhub.config.Loggable
import com.jonquass.guardianhub.core.auth.ChangePasswordRequest
import com.jonquass.guardianhub.core.auth.LoginRequest
import com.jonquass.guardianhub.core.auth.LoginResponse
import com.jonquass.guardianhub.core.auth.ResetToFactoryRequest
import com.jonquass.guardianhub.core.config.Env
import com.jonquass.guardianhub.core.manager.Result
import com.jonquass.guardianhub.manager.ConfigManager
import jakarta.ws.rs.core.Response
import java.io.File

object AuthManager : Loggable {
    private val logger = logger()

    internal const val DEFAULT_FACTORY_PASSWORD_PATH = "/opt/pi-stack/.factory-password"
    internal const val DEFAULT_SERIAL_NUMBER_PATH = "/opt/pi-stack/.serial-number"

    internal var factoryPasswordFile = File(DEFAULT_FACTORY_PASSWORD_PATH)
    internal var serialNumberFile = File(DEFAULT_SERIAL_NUMBER_PATH)

    fun login(request: LoginRequest): Result<LoginResponse> {
        val isValid = validatePassword(request.password)

        if (isValid) {
            val token = SessionManager.createSession()
            return Result
                .Success(LoginResponse(success = true, token = token))
        } else {
            return Result.Error("Invalid password", Response.Status.UNAUTHORIZED)
        }
    }

    fun resetToFactory(request: ResetToFactoryRequest): Result<Map<String, Any>> {
        val success =
            resetToFactory(
                request.factoryPassword,
                request.serialNumber,
            )

        if (success) {
            return Result.Success(
                mapOf(
                    "success" to true,
                    "message" to "Password reset to factory default",
                ),
            )
        }

        return Result.Error(
            "Invalid factory password or serial number",
            Response.Status.BAD_REQUEST,
        )
    }

    fun changePassword(
        authHeader: String?,
        request: ChangePasswordRequest,
    ): Result<Map<String, Any>> {
        val isAuthValid = isAuthValid(authHeader)
        if (!isAuthValid) {
            return Result.Error("Unauthorized", Response.Status.UNAUTHORIZED)
        }

        val success =
            changePassword(
                request.currentPassword,
                request.newPassword,
                request.serialNumber,
            )
        if (success) {
            return Result.Success(
                mapOf(
                    "success" to true,
                    "message" to "Login password changed successfully",
                ),
            )
        }

        return Result.Error(
            "Failed to change password. Check current password, serial number, and ensure new password is at least 8 characters.",
            Response.Status.BAD_REQUEST,
        )
    }

    fun logout(authHeader: String?): Result<Map<String, Any>> {
        val token = getToken(authHeader)
        if (token != null) {
            SessionManager.invalidateSession(token)
        }
        return Result.Success(mapOf("success" to true))
    }

    fun checkAuth(authHeader: String?): Result<Map<String, Any>> {
        val isAuthValid = isAuthValid(authHeader)
        return Result.Success(mapOf("authenticated" to isAuthValid))
    }

    fun getToken(authHeader: String?): String? = authHeader?.removePrefix("Bearer ")?.trim()

    private fun isAuthValid(authHeader: String?): Boolean {
        val token = getToken(authHeader)
        return token?.let { SessionManager.isValidSession(it) } ?: false
    }

    private fun validatePassword(password: String): Boolean {
        val loginPasswordHash = ConfigManager.getRawConfigValue(Env.LOGIN_PASSWORD)
        if (loginPasswordHash.isNullOrEmpty()) {
            logger.warn("Login password hash not found in .env")
            return false
        }

        return PasswordHashManager.verifyHash(password, loginPasswordHash)
    }

    private fun validateFactoryPassword(password: String): Boolean {
        if (!factoryPasswordFile.exists()) {
            logger.warn("Factory password file not found")
            return false
        }

        val factoryPasswordHash = factoryPasswordFile.readText().trim()
        return PasswordHashManager.verifyHash(password, factoryPasswordHash)
    }

    private fun validateSerialNumber(serial: String): Boolean {
        if (!serialNumberFile.exists()) {
            logger.warn("Serial number file not found")
            return false
        }

        val serialHash = serialNumberFile.readText().trim()
        return PasswordHashManager.verifyHash(serial, serialHash)
    }

    private fun resetToFactory(
        factoryPassword: String,
        serialNumber: String,
    ): Boolean {
        return try {
            if (!validateFactoryPassword(factoryPassword)) {
                logger.error("Invalid factory password provided for reset")
                return false
            }

            if (!validateSerialNumber(serialNumber)) {
                logger.error("Invalid serial number provided for reset")
                return false
            }

            val factoryHash = factoryPasswordFile.readText().trim()
            ConfigManager.upsertConfig(Env.LOGIN_PASSWORD, factoryHash)
            SessionManager.invalidateSessions()
            logger.info("Login password reset to factory password")
            true
        } catch (e: Exception) {
            logger.error("Failed to reset password: {}", e.message, e)
            throw e
        }
    }

    private fun changePassword(
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

            val newPasswordHash = PasswordHashManager.hashPassword(newPassword)

            ConfigManager.upsertConfig(Env.LOGIN_PASSWORD, newPasswordHash)

            logger.info("Login password changed successfully")
            true
        } catch (e: Exception) {
            logger.error("Failed to change password: {}", e.message, e)
            throw e
        }
    }
}
