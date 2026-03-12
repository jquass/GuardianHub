package com.jonquass.guardianhub.manager

import com.jonquass.guardianhub.config.Loggable
import com.jonquass.guardianhub.core.Result
import com.jonquass.guardianhub.core.api.UpdatePasswordRequest
import com.jonquass.guardianhub.core.api.UpdatePasswordResponse
import com.jonquass.guardianhub.core.config.Env
import com.jonquass.guardianhub.core.errOrThrow
import com.jonquass.guardianhub.core.getOrThrow
import com.jonquass.guardianhub.validator.PasswordValidator
import jakarta.ws.rs.core.Response

object PasswordManager : Loggable {
  private val logger = logger()

  fun updatePiholePassword(request: UpdatePasswordRequest): Result<UpdatePasswordResponse> {
    val validationResult = PasswordValidator.validate(request.password)
    if (validationResult.isError) {
      return validationResult.errOrThrow()
    }

    logger.info("Updating Pi-hole password")
    ConfigManager.upsertConfig(Env.PIHOLE_PASSWORD, request.password)

    val setPwdResult =
        DockerManager.exec(
            "/usr/bin/docker",
            "exec",
            "pihole",
            "pihole",
            "setpassword",
            request.password,
        )
    if (setPwdResult.isError) {
      return Result.error(
          "Password updated in .env but failed to set in Pi-hole container. " +
              "Try manually: docker exec pihole pihole setpassword 'yourpassword'",
      )
    }

    return Result.success(UpdatePasswordResponse("Pi-hole password updated successfully", false))
  }

  fun updateWireGuardPassword(request: UpdatePasswordRequest): Result<UpdatePasswordResponse> {
    val validationResult = PasswordValidator.validate(request.password)
    if (validationResult.isError) {
      return validationResult.errOrThrow()
    }

    logger.info("Updating WireGuard password in .env file...")

    val hash =
        hashWireGuardPassword(request.password)
            ?: return Result.error("Failed to generate WireGuard password hash")

    ConfigManager.upsertConfig(Env.WIREGUARD_PASSWORD_HASH, hash)
    logger.info("Updated .env file")

    val recreated = DockerManager.recreateContainer("wireguard").isSuccess
    logger.info("WireGuard recreated: {}", recreated)

    return Result.success(
        UpdatePasswordResponse("WireGuard password updated successfully", recreated))
  }

  fun updateNpmPassword(request: UpdatePasswordRequest): Result<UpdatePasswordResponse> {
    if (request.password.isBlank()) {
      return Result.error("Password cannot be empty", Response.Status.BAD_REQUEST)
    }
    if (request.password.length < 8) {
      return Result.error("Password must be at least 8 characters", Response.Status.BAD_REQUEST)
    }

    logger.info("Updating NPM password...")

    val currentEmailResult = ConfigManager.getRawConfigValue(Env.NPM_ADMIN_EMAIL)
    if (currentEmailResult.isError) {
      return currentEmailResult.errOrThrow()
    }
    val currentEmail = currentEmailResult.getOrThrow()

    val currentPasswordResult = ConfigManager.getRawConfigValue(Env.NPM_ADMIN_PASSWORD)
    if (currentPasswordResult.isError) {
      return currentPasswordResult.errOrThrow()
    }
    val currentPassword = currentPasswordResult.getOrThrow()

    val token =
        fetchNpmToken(currentEmail, currentPassword)
            ?: return Result.error(
                "Failed to authenticate with NPM. The email in NPM must match NPM_ADMIN_EMAIL ($currentEmail).",
                Response.Status.UNAUTHORIZED,
            )

    val userId =
        fetchNpmUserId(token, currentEmail)
            ?: return Result.error(
                "Failed to find NPM user with email: $currentEmail.",
            )

    val updateSuccess = updateNpmUserPassword(userId, token, currentPassword, request.password)
    if (!updateSuccess) {
      return Result.error("Failed to update NPM password via API")
    }

    ConfigManager.upsertConfig(Env.NPM_ADMIN_PASSWORD, request.password)

    return Result.success(UpdatePasswordResponse("NPM password updated successfully", false))
  }

  private fun fetchNpmToken(
      email: String,
      password: String,
  ): String? {
    val authJson = """{"identity":"$email","secret":"$password"}"""
    val result =
        DockerManager.execWithOutput(
            "/usr/bin/curl",
            "-s",
            "-X",
            "POST",
            "http://172.20.0.5:81/api/tokens",
            "-H",
            "Content-Type: application/json",
            "-d",
            authJson,
        )
    if (result.isError) {
      return null
    }
    val output = result.getOrThrow()

    return """"token":"([^"]+)"""".toRegex().find(output)?.groupValues?.get(1)
  }

  private fun fetchNpmUserId(
      token: String,
      email: String,
  ): String? {
    val result =
        DockerManager.execWithOutput(
            "/usr/bin/curl",
            "-s",
            "-X",
            "GET",
            "http://172.20.0.5:81/api/users",
            "-H",
            "Authorization: Bearer $token",
        )
    if (result.isError) {
      return null
    }
    val output = result.getOrThrow()

    return """"id":(\d+)[^}]*"email":"${Regex.escape(email)}"""
        .toRegex(RegexOption.DOT_MATCHES_ALL)
        .find(output)
        ?.groupValues
        ?.get(1)
  }

  private fun updateNpmUserPassword(
      userId: String,
      token: String,
      currentPassword: String,
      newPassword: String,
  ): Boolean {
    val updateJson = """{"type":"password","current":"$currentPassword","secret":"$newPassword"}"""
    val result =
        DockerManager.execWithOutput(
            "/usr/bin/curl",
            "-s",
            "-X",
            "PUT",
            "http://172.20.0.5:81/api/users/$userId/auth",
            "-H",
            "Authorization: Bearer $token",
            "-H",
            "Content-Type: application/json",
            "-d",
            updateJson,
        )
    if (result.isError) {
      return false
    }
    val output = result.getOrThrow()

    return output.trim() == "true"
  }

  private fun hashWireGuardPassword(password: String): String? {
    val result =
        DockerManager.execWithOutput(
            "docker",
            "run",
            "--rm",
            "ghcr.io/wg-easy/wg-easy:latest",
            "wgpw",
            password,
        )
    if (result.isError) {
      return null
    }
    val output = result.getOrThrow()

    return Regex("PASSWORD_HASH=(.*)").find(output)?.groupValues?.get(1)
  }
}
