package com.jonquass.guardianhub.managers

import com.jonquass.guardianhub.config.Loggable
import com.jonquass.guardianhub.core.api.UpdateTimezoneRequest
import com.jonquass.guardianhub.core.config.Env
import jakarta.ws.rs.core.Response
import java.time.ZoneId

object TimezoneManager : Loggable {
    private val logger = logger()

    // Cache of valid timezone IDs
    private val validTimezones: Set<String> by lazy {
        ZoneId.getAvailableZoneIds()
    }

    fun getTimezones(): Response =
        try {
            val timezones = validTimezones.sorted()

            Response
                .ok(
                    mapOf(
                        "status" to "success",
                        "timezones" to timezones,
                    ),
                ).build()
        } catch (e: Exception) {
            logger.error("Failed to get timezones: {}", e.message, e)
            Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(
                    mapOf(
                        "status" to "error",
                        "message" to "Failed to get timezones: ${e.message}",
                    ),
                ).build()
        }

    fun isValidTimezone(timezone: String): Boolean = validTimezones.contains(timezone)

    fun updateTimezones(request: UpdateTimezoneRequest): Response {
        return try {
            // Validate timezone is in the list of valid timezones
            if (!isValidTimezone(request.timezone)) {
                logger.warn("Invalid timezone attempted: {}", request.timezone)
                return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(
                        mapOf(
                            "status" to "error",
                            "message" to "Invalid timezone: ${request.timezone}. Must be a valid timezone from the list.",
                        ),
                    ).build()
            }

            logger.info("Updating timezone to: {}", request.timezone)

            // Update .env file
            ConfigManager.upsertConfig(Env.TZ, request.timezone)

            // Services to restart (only those that use TZ)
            val servicesToRestart =
                listOf(
                    "cloudflared",
                    "pihole",
                )

            // Start async restart and get task ID
            val taskId = ServiceStatusManager.restartServicesAsync(servicesToRestart)

            Response
                .ok(
                    mapOf(
                        "status" to "success",
                        "message" to "Timezone updated to ${request.timezone}. Services are restarting.",
                        "taskId" to taskId,
                    ),
                ).build()
        } catch (e: Exception) {
            logger.error("Failed to update timezone: {}", e.message, e)
            Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(
                    mapOf(
                        "status" to "error",
                        "message" to "Failed to update timezone: ${e.message}",
                    ),
                ).build()
        }
    }
}
