package com.jonquass.guardianhub.manager

import com.jonquass.guardianhub.config.Loggable
import com.jonquass.guardianhub.core.Result
import com.jonquass.guardianhub.core.api.UpdateTimezoneRequest
import com.jonquass.guardianhub.core.config.Env
import jakarta.ws.rs.core.Response
import java.time.ZoneId

object TimezoneManager : Loggable {
  private val logger = logger()

  // Cache of valid timezone IDs
  private val validTimezones: Set<String> by lazy { ZoneId.getAvailableZoneIds() }
  private val sortedTimezones: List<String> by lazy { validTimezones.sorted() }

  fun getTimezonesResult(): Result<Map<String, Any>> =
      try {
        return Result.Success(
            mapOf(
                "status" to "success",
                "timezones" to sortedTimezones,
            ))
      } catch (e: Exception) {
        logger.error("Failed to get timezones: {}", e.message, e)
        return Result.Error("Failed to get timezones")
      }

  fun isValidTimezone(timezone: String): Boolean = validTimezones.contains(timezone)

  fun updateTimezonesResult(request: UpdateTimezoneRequest): Result<Map<String, Any>> {
    return try {
      // Validate timezone is in the list of valid timezones
      if (!isValidTimezone(request.timezone)) {
        logger.warn("Invalid timezone attempted: {}", request.timezone)
        return Result.Error(
            "Invalid timezone: ${request.timezone}. Must be a valid timezone from the list.",
            Response.Status.BAD_REQUEST)
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

      Result.Success(
          mapOf(
              "status" to "success",
              "message" to "Timezone updated to ${request.timezone}. Services are restarting.",
              "taskId" to taskId,
          ))
    } catch (e: Exception) {
      logger.error("Failed to update timezone: {}", e.message, e)
      Result.Error("Failed to update timezone")
    }
  }
}
