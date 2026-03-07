package com.jonquass.guardianhub.manager

import com.jonquass.guardianhub.config.Loggable
import com.jonquass.guardianhub.core.ExcludeManagerCheck
import com.jonquass.guardianhub.core.Result
import com.jonquass.guardianhub.core.api.TimezoneResponse
import com.jonquass.guardianhub.core.api.UpdateTimezoneRequest
import com.jonquass.guardianhub.core.config.Env
import jakarta.ws.rs.core.Response
import java.time.ZoneId

object TimezoneManager : Loggable {
  private val logger = logger()

  // Cache of valid timezone IDs
  private val validTimezones: Set<String> by lazy { ZoneId.getAvailableZoneIds() }
  private val sortedTimezones: List<String> by lazy { validTimezones.sorted() }
  private val servicesToRestart = listOf("cloudflared", "pihole")

  fun getTimezonesResult(): Result<TimezoneResponse> =
      Result.success(TimezoneResponse(sortedTimezones))

  @ExcludeManagerCheck
  fun isValidTimezone(timezone: String): Boolean = validTimezones.contains(timezone)

  fun updateTimezonesResult(request: UpdateTimezoneRequest): Result<Map<String, Any>> {
    return try {
      // Validate timezone is in the list of valid timezones
      if (!isValidTimezone(request.timezone)) {
        logger.warn("Invalid timezone attempted: {}", request.timezone)
        return Result.error(
            "Invalid timezone: ${request.timezone}. Must be a valid timezone from the list.",
            Response.Status.BAD_REQUEST)
      }

      logger.info("Updating timezone to: {}", request.timezone)
      ConfigManager.upsertConfig(Env.TZ, request.timezone)
      val taskId = ServiceStatusManager.restartServicesAsync(servicesToRestart)
      Result.success(
          mapOf(
              "status" to "success",
              "message" to "Timezone updated to ${request.timezone}. Services are restarting.",
              "taskId" to taskId,
          ))
    } catch (e: Exception) {
      logger.error("Failed to update timezone: {}", e.message, e)
      Result.error("Failed to update timezone")
    }
  }
}
