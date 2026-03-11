package com.jonquass.guardianhub.manager

import com.jonquass.guardianhub.config.Loggable
import com.jonquass.guardianhub.core.Result
import com.jonquass.guardianhub.core.api.TimezoneResponse
import com.jonquass.guardianhub.core.api.UpdateTimezoneRequest
import com.jonquass.guardianhub.core.api.UpdateTimezoneResponse
import com.jonquass.guardianhub.core.config.Env
import com.jonquass.guardianhub.core.errOrThrow
import jakarta.ws.rs.core.Response
import java.time.ZoneId

object TimezoneManager : Loggable {
  private val logger = logger()

  private val validTimezones: Set<String> by lazy { ZoneId.getAvailableZoneIds() }
  private val sortedTimezones: List<String> by lazy { validTimezones.sorted() }
  private val servicesToRestart = listOf("cloudflared", "pihole")

  fun getTimezonesResult(): Result<TimezoneResponse> =
      Result.success(TimezoneResponse(sortedTimezones))

  fun isValidTimezoneResult(timezone: String): Result<Unit> {
    if (!validTimezones.contains(timezone)) {
      return Result.error("Invalid timezone: $timezone.", Response.Status.BAD_REQUEST)
    }

    return Result.success()
  }

  fun updateTimezonesResult(request: UpdateTimezoneRequest): Result<UpdateTimezoneResponse> {
    return try {
      val result = isValidTimezoneResult(request.timezone)
      if (result.isError) {
        logger.warn("Invalid timezone attempted: {}", request.timezone)
        return result.errOrThrow()
      }

      logger.info("Updating timezone to: {}", request.timezone)
      ConfigManager.upsertConfig(Env.TZ, request.timezone)
      val taskId = ServiceStatusManager.restartServicesAsync(servicesToRestart)
      Result.success(
          UpdateTimezoneResponse(
              "Timezone updated to ${request.timezone}. Services are restarting.", taskId))
    } catch (e: Exception) {
      logger.error("Failed to update timezone: {}", e.message, e)
      Result.error("Failed to update timezone")
    }
  }
}
