package com.jonquass.guardianhub.manager.auth

import com.jonquass.guardianhub.config.Loggable
import com.jonquass.guardianhub.core.Result
import com.jonquass.guardianhub.core.api.auth.ChangePasswordRequest
import com.jonquass.guardianhub.core.api.auth.ChangePasswordResponse
import com.jonquass.guardianhub.core.api.auth.CheckAuthResponse
import com.jonquass.guardianhub.core.api.auth.LoginRequest
import com.jonquass.guardianhub.core.api.auth.LoginResponse
import com.jonquass.guardianhub.core.api.auth.ResetToFactoryRequest
import com.jonquass.guardianhub.core.api.auth.ResetToFactoryResponse
import com.jonquass.guardianhub.core.config.Env
import com.jonquass.guardianhub.core.getOrThrow
import com.jonquass.guardianhub.core.orError
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
      return Result.success(LoginResponse(success = true, token = token))
    } else {
      return Result.error("Invalid password", Response.Status.UNAUTHORIZED)
    }
  }

  fun resetToFactory(request: ResetToFactoryRequest): Result<ResetToFactoryResponse> {
    val success =
        resetToFactory(
            request.factoryPassword,
            request.serialNumber,
        )

    if (success) {
      return Result.success(ResetToFactoryResponse("Password reset to factory default"))
    }

    return Result.error(
        "Invalid factory password or serial number",
        Response.Status.BAD_REQUEST,
    )
  }

  fun changePassword(
      authHeader: String?,
      request: ChangePasswordRequest,
  ): Result<ChangePasswordResponse> {
    val isAuthValid = isAuthValid(authHeader)
    if (!isAuthValid) {
      return Result.error("Unauthorized", Response.Status.UNAUTHORIZED)
    }

    val success =
        changePassword(
            request.currentPassword,
            request.newPassword,
            request.serialNumber,
        )
    if (success) {
      return Result.success(ChangePasswordResponse("Login password changed successfully"))
    }

    return Result.error(
        "Failed to change password. Check current password, serial number, and ensure new password is at least 8 characters.",
        Response.Status.BAD_REQUEST,
    )
  }

  fun logout(authHeader: String?): Result<Unit> {
    val result = getToken(authHeader)
    if (result.isSuccess) {
      SessionManager.invalidateSession(result.getOrThrow())
    }
    return Result.success()
  }

  fun checkAuth(authHeader: String?): Result<CheckAuthResponse> {
    val isAuthValid = isAuthValid(authHeader)
    return Result.success(CheckAuthResponse(isAuthValid))
  }

  fun getToken(authHeader: String?): Result<String> =
      authHeader
          ?.removePrefix("Bearer ")
          ?.trim()
          ?.takeIf { it.isNotEmpty() }
          .orError(
              errorMessage = "Missing or empty Authorization header",
              code = Response.Status.UNAUTHORIZED)

  private fun isAuthValid(authHeader: String?): Boolean {
    val result = getToken(authHeader)
    if (result.isError) {
      return false
    }

    return SessionManager.isValidSession(result.getOrThrow())
  }

  private fun validatePassword(password: String): Boolean {
    val loginPasswordHashResult = ConfigManager.getRawConfigValue(Env.LOGIN_PASSWORD)
    if (loginPasswordHashResult is Result.Error) {
      return false
    }

    val loginPasswordHash = loginPasswordHashResult.getOrThrow()
    return PasswordHashManager.verifyHash(password, loginPasswordHash).isSuccess
  }

  private fun validateFactoryPassword(password: String): Boolean {
    if (!factoryPasswordFile.exists()) {
      logger.warn("Factory password file not found")
      return false
    }

    val factoryPasswordHash = factoryPasswordFile.readText().trim()
    return PasswordHashManager.verifyHash(password, factoryPasswordHash).isSuccess
  }

  private fun validateSerialNumber(serial: String): Boolean {
    if (!serialNumberFile.exists()) {
      logger.warn("Serial number file not found")
      return false
    }

    val serialHash = serialNumberFile.readText().trim()
    return PasswordHashManager.verifyHash(serial, serialHash).isSuccess
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

      val hashResult = PasswordHashManager.hashPasswordResult(newPassword)
      if (hashResult.isError) {
        return false
      }

      ConfigManager.upsertConfig(Env.LOGIN_PASSWORD, hashResult.getOrThrow())

      logger.info("Login password changed successfully")
      true
    } catch (e: Exception) {
      logger.error("Failed to change password: {}", e.message, e)
      throw e
    }
  }
}
