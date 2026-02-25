package com.jonquass.guardianhub.managers

import com.jonquass.guardianhub.config.Loggable
import com.jonquass.guardianhub.core.api.UpdatePasswordRequest
import com.jonquass.guardianhub.core.api.UpdatePasswordResponse
import com.jonquass.guardianhub.core.config.Env
import com.jonquass.guardianhub.core.manager.Result
import com.jonquass.guardianhub.validators.PasswordValidator
import jakarta.ws.rs.core.Response

object PasswordManager : Loggable {
    private val logger = logger()

    fun updatePiholePassword(request: UpdatePasswordRequest): Result<UpdatePasswordResponse> {
        val validationResult = PasswordValidator.validate(request.password)
        if (validationResult.errorResponse != null) {
            return Result.Error(validationResult.errorResponse.toString(), Response.Status.BAD_REQUEST)
        }

        logger.info("Updating Pi-hole password")
        ConfigManager.upsertConfig(Env.PIHOLE_PASSWORD, request.password)

        val setPwdSuccess =
            DockerManager.exec(
                "/usr/bin/docker",
                "exec",
                "pihole",
                "pihole",
                "setpassword",
                request.password,
            )
        if (!setPwdSuccess) {
            return Result.Error(
                "Password updated in .env but failed to set in Pi-hole container. " +
                    "Try manually: docker exec pihole pihole setpassword 'yourpassword'",
            )
        }

        return Result.Success(
            UpdatePasswordResponse(
                status = "success",
                message = "Pi-hole password updated successfully",
                serviceRestarted = false,
            ),
        )
    }

    fun updateWireGuardPassword(request: UpdatePasswordRequest): Result<UpdatePasswordResponse> {
        val validationResult = PasswordValidator.validate(request.password)
        if (validationResult.errorResponse != null) {
            return Result.Error(validationResult.errorResponse.toString(), Response.Status.BAD_REQUEST)
        }

        logger.info("Updating WireGuard password in .env file...")

        val hash =
            hashWireGuardPassword(request.password)
                ?: return Result.Error("Failed to generate WireGuard password hash")

        ConfigManager.upsertConfig(Env.WIREGUARD_PASSWORD_HASH, hash)
        logger.info("Updated .env file")

        val recreated = DockerManager.recreateContainer("wireguard")
        logger.info("WireGuard recreated: {}", recreated)

        return Result.Success(
            UpdatePasswordResponse(
                status = "success",
                message = "WireGuard password updated successfully",
                serviceRestarted = recreated,
            ),
        )
    }

    fun updateNpmPassword(request: UpdatePasswordRequest): Result<UpdatePasswordResponse> {
        if (request.password.isBlank()) {
            return Result.Error("Password cannot be empty", Response.Status.BAD_REQUEST)
        }
        if (request.password.length < 8) {
            return Result.Error("Password must be at least 8 characters", Response.Status.BAD_REQUEST)
        }

        logger.info("Updating NPM password...")

        val currentEmail =
            ConfigManager.getRawConfigValue(Env.NPM_ADMIN_EMAIL)
                ?: return Result.Error(
                    "NPM credentials not configured. Please add NPM_ADMIN_EMAIL and NPM_ADMIN_PASSWORD to .env",
                )

        val currentPassword =
            ConfigManager.getRawConfigValue(Env.NPM_ADMIN_PASSWORD)
                ?: return Result.Error(
                    "NPM credentials not configured. Please add NPM_ADMIN_EMAIL and NPM_ADMIN_PASSWORD to .env",
                )

        val token =
            fetchNpmToken(currentEmail, currentPassword)
                ?: return Result.Error(
                    "Failed to authenticate with NPM. The email in NPM must match NPM_ADMIN_EMAIL ($currentEmail).",
                    Response.Status.UNAUTHORIZED,
                )

        val userId =
            fetchNpmUserId(token, currentEmail)
                ?: return Result.Error(
                    "Failed to find NPM user with email: $currentEmail.",
                )

        val updateSuccess = updateNpmUserPassword(userId, token, currentPassword, request.password)
        if (!updateSuccess) {
            return Result.Error("Failed to update NPM password via API")
        }

        ConfigManager.upsertConfig(Env.NPM_ADMIN_PASSWORD, request.password)

        return Result.Success(
            UpdatePasswordResponse(
                status = "success",
                message = "NPM password updated successfully",
                serviceRestarted = false,
            ),
        )
    }

    private fun fetchNpmToken(
        email: String,
        password: String,
    ): String? {
        val authJson = """{"identity":"$email","secret":"$password"}"""
        val output =
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
        if (output.first != 0 || output.second.isNullOrEmpty()) return null

        return """"token":"([^"]+)"""".toRegex().find(output.second!!)?.groupValues?.get(1)
    }

    private fun fetchNpmUserId(
        token: String,
        email: String,
    ): String? {
        val output =
            DockerManager.execWithOutput(
                "/usr/bin/curl",
                "-s",
                "-X",
                "GET",
                "http://172.20.0.5:81/api/users",
                "-H",
                "Authorization: Bearer $token",
            )
        if (output.first != 0 || output.second.isNullOrEmpty()) return null

        return """"id":(\d+)[^}]*"email":"${Regex.escape(email)}"""
            .toRegex(RegexOption.DOT_MATCHES_ALL)
            .find(output.second!!)
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
        val output =
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
        if (output.first != 0 || output.second.isNullOrEmpty()) return false

        return output.second!!.trim() == "true"
    }

    private fun hashWireGuardPassword(password: String): String? {
        val output =
            DockerManager.execWithOutput(
                "docker",
                "run",
                "--rm",
                "ghcr.io/wg-easy/wg-easy:latest",
                "wgpw",
                password,
            )
        if (output.first != 0 || output.second.isNullOrEmpty()) return null

        return Regex("PASSWORD_HASH=(.*)").find(output.second!!)?.groupValues?.get(1)
    }
}
