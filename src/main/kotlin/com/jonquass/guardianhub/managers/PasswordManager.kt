package com.jonquass.guardianhub.managers

import com.jonquass.guardianhub.config.Loggable
import com.jonquass.guardianhub.core.api.UpdatePasswordRequest
import com.jonquass.guardianhub.core.api.UpdatePasswordResponse
import com.jonquass.guardianhub.core.config.Env
import com.jonquass.guardianhub.validators.PasswordValidator
import jakarta.ws.rs.core.Response
import java.io.BufferedReader
import java.io.InputStreamReader

object PasswordManager : Loggable {
    private val logger = logger()

    fun updatePiholePassword(request: UpdatePasswordRequest): Response {
        return try {
            val validationResult = PasswordValidator.validate(request.password)
            if (validationResult.errorResponse != null) {
                return validationResult.errorResponse
            }

            logger.info("Updating Pi-hole password")

            ConfigManager.upsertConfig(Env.PIHOLE_PASSWORD, request.password)

            val setPwdSuccess = PiholePasswordManager.setPiholePassword(request.password)

            if (!setPwdSuccess) {
                return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(
                        mapOf(
                            "status" to "error",
                            "message" to
                                "Password updated in .env but failed to set in Pi-hole container. Try manually: docker exec pihole pihole setpassword 'yourpassword'",
                        ),
                    ).build()
            }

            val response =
                UpdatePasswordResponse(
                    status = "success",
                    message = "Pi-hole password updated successfully",
                    serviceRestarted = false,
                )

            Response.ok(response).build()
        } catch (e: Exception) {
            e.printStackTrace()
            Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(
                    mapOf(
                        "status" to "error",
                        "message" to "Error: ${e.message}",
                    ),
                ).build()
        }
    }

    fun updateWireGuardPassword(request: UpdatePasswordRequest): Response {
        return try {
            val validationResult = PasswordValidator.validate(request.password)
            if (validationResult.errorResponse != null) {
                return validationResult.errorResponse
            }

            logger.info("Updating WireGuard password in .env file...")

            val hash = hashWireGuardPassword(request.password)
            ConfigManager.upsertConfig(Env.WIREGUARD_PASSWORD_HASH, hash)

            logger.info("Updated .env file")

            val recreated = recreateContainer("wireguard")
            logger.info("WireGuard recreated: {}", recreated)

            val response =
                UpdatePasswordResponse(
                    status = "success",
                    message = "WireGuard password updated successfully",
                    serviceRestarted = recreated,
                )

            Response.ok(response).build()
        } catch (e: Exception) {
            e.printStackTrace()
            Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(
                    mapOf(
                        "status" to "error",
                        "message" to "Error: ${e.message}",
                    ),
                ).build()
        }
    }

    /**
     * Hash a plain text password for WireGuard using bcrypt
     */
    private fun hashWireGuardPassword(plainPassword: String): String {
        try {
            val process =
                ProcessBuilder(
                    "docker",
                    "run",
                    "--rm",
                    "ghcr.io/wg-easy/wg-easy:latest",
                    "wgpw",
                    plainPassword,
                ).start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readLine()?.trim()

            val exitCode = process.waitFor()
            if (exitCode != 0 || output.isNullOrEmpty()) {
                throw Exception("Failed to generate WireGuard password hash")
            }

            val hashMatch = Regex("PASSWORD_HASH=(.*)").find(output)
            val hash =
                hashMatch?.groupValues?.get(1)
                    ?: throw Exception("Could not parse password hash from output: $output")

            return hash
        } catch (e: Exception) {
            throw Exception("Error hashing WireGuard password: ${e.message}")
        }
    }

    private fun recreateContainer(serviceName: String): Boolean =
        try {
            logger.info("Recreating service {} to reload environment...", serviceName)

            // Stop and remove the container
            val stopProcess =
                ProcessBuilder(
                    "/usr/bin/docker",
                    "compose",
                    "-f",
                    "/opt/pi-stack/docker-compose.yml",
                    "stop",
                    serviceName,
                ).redirectErrorStream(true)
                    .start()

            stopProcess.waitFor()

            val rmProcess =
                ProcessBuilder(
                    "/usr/bin/docker",
                    "compose",
                    "-f",
                    "/opt/pi-stack/docker-compose.yml",
                    "rm",
                    "-f",
                    serviceName,
                ).redirectErrorStream(true)
                    .start()

            rmProcess.waitFor()

            val upProcess =
                ProcessBuilder(
                    "/usr/bin/docker",
                    "compose",
                    "-f",
                    "/opt/pi-stack/docker-compose.yml",
                    "up",
                    "-d",
                    serviceName,
                ).redirectErrorStream(true)
                    .start()

            val output = upProcess.inputStream.bufferedReader().use { it.readText() }
            val exitCode = upProcess.waitFor()

            logger.info("Service recreate exit code: {}", exitCode)
            logger.info("Output: {}", output)

            exitCode == 0
        } catch (e: Exception) {
            logger.error("Failed to restart {}: {}", serviceName, e.message, e)
            false
        }

    fun updateNpmPassword(request: UpdatePasswordRequest): Response {
        return try {
            if (request.password.isBlank()) {
                return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(
                        mapOf(
                            "status" to "error",
                            "message" to "Password cannot be empty",
                        ),
                    ).build()
            }

            if (request.password.length < 8) {
                return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(
                        mapOf(
                            "status" to "error",
                            "message" to "Password must be at least 8 characters",
                        ),
                    ).build()
            }

            logger.info("Updating NPM password...")

            val currentEmail = ConfigManager.getRawConfigValue(Env.NPM_ADMIN_EMAIL)
            val currentPassword = ConfigManager.getRawConfigValue(Env.NPM_ADMIN_PASSWORD)

            if (currentEmail == null || currentPassword == null) {
                logger.error("NPM credentials not found in .env")
                return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(
                        mapOf(
                            "status" to "error",
                            "message" to "NPM credentials not configured. Please add NPM_ADMIN_EMAIL and NPM_ADMIN_PASSWORD to .env",
                        ),
                    ).build()
            }

            logger.info("Using email: {}", currentEmail)

            // Authenticate with NPM API
            val authJson = """{"identity":"$currentEmail","secret":"$currentPassword"}"""

            val tokenProcess =
                ProcessBuilder(
                    "/usr/bin/curl",
                    "-s",
                    "-X",
                    "POST",
                    "http://172.20.0.5:81/api/tokens",
                    "-H",
                    "Content-Type: application/json",
                    "-d",
                    authJson,
                ).start()

            val tokenOutput = tokenProcess.inputStream.bufferedReader().readText()
            tokenProcess.waitFor()

            logger.info("Token response: {}", tokenOutput)

            // Extract token from response
            val tokenRegex = """"token":"([^"]+)"""".toRegex()
            val token = tokenRegex.find(tokenOutput)?.groupValues?.get(1)

            if (token == null) {
                logger.error("Failed to authenticate with NPM")
                return Response
                    .status(Response.Status.UNAUTHORIZED)
                    .entity(
                        mapOf(
                            "status" to "error",
                            "message" to
                                "Failed to authenticate with NPM. The email in NPM must match NPM_ADMIN_EMAIL ($currentEmail). Please update the email in NPM via the web UI.",
                        ),
                    ).build()
            }

            logger.info("Authentication successful")

            // Get admin user ID
            val getUserProcess =
                ProcessBuilder(
                    "/usr/bin/curl",
                    "-s",
                    "-X",
                    "GET",
                    "http://172.20.0.5:81/api/users",
                    "-H",
                    "Authorization: Bearer $token",
                ).start()

            val usersOutput = getUserProcess.inputStream.bufferedReader().readText()
            getUserProcess.waitFor()

            logger.info("Users API response: {}", usersOutput)

            // Parse to find user ID
            val userPattern = """"id":(\d+)[^}]*"email":"${Regex.escape(currentEmail)}"""".toRegex(RegexOption.DOT_MATCHES_ALL)
            val userId = userPattern.find(usersOutput)?.groupValues?.get(1)

            if (userId == null) {
                logger.error("Failed to find user with email: {}", currentEmail)
                return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(
                        mapOf(
                            "status" to "error",
                            "message" to
                                "Failed to find NPM user with email: $currentEmail. Please ensure the email in NPM matches NPM_ADMIN_EMAIL in .env",
                        ),
                    ).build()
            }

            logger.info("Found user ID: {} for email: {}", userId, currentEmail)

            // Update password via /auth endpoint
            val updateJson = """{"type":"password","current":"$currentPassword","secret":"${request.password}"}"""

            logger.info("Sending password update request to user ID: {}", userId)

            val updateProcess =
                ProcessBuilder(
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
                ).start()

            val updateOutput = updateProcess.inputStream.bufferedReader().readText()
            val updateExitCode = updateProcess.waitFor()

            logger.info("Update API exit code: {}", updateExitCode)
            logger.info("Update API response: {}", updateOutput)

            // Check if response is "true" (success)
            if (updateOutput.trim() != "true") {
                logger.error("Update API did not return true")
                return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(
                        mapOf(
                            "status" to "error",
                            "message" to "Failed to update NPM password. API response: $updateOutput",
                        ),
                    ).build()
            }

            // Update .env file
            ConfigManager.upsertConfig(Env.NPM_ADMIN_PASSWORD, request.password)

            val response =
                UpdatePasswordResponse(
                    status = "success",
                    message = "NPM password updated successfully",
                    serviceRestarted = false,
                )

            Response.ok(response).build()
        } catch (e: Exception) {
            e.printStackTrace()
            Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(
                    mapOf(
                        "status" to "error",
                        "message" to "Error: ${e.message}",
                    ),
                ).build()
        }
    }
}
