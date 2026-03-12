package com.jonquass.guardianhub.resource

import com.jonquass.guardianhub.core.api.TimezoneResponse
import com.jonquass.guardianhub.core.api.UpdateTimezoneRequest
import com.jonquass.guardianhub.core.api.UpdateTimezoneResponse
import com.jonquass.guardianhub.core.toResponse
import com.jonquass.guardianhub.manager.TimezoneManager
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/timezone")
class TimezoneResource {
  @GET
  @Operation(
      summary = "Get all timezones.",
      security = [SecurityRequirement(name = "bearerAuth")],
      responses =
          [
              ApiResponse(
                  responseCode = "200",
                  description = "Fetched timezones successfully",
                  content = [Content(schema = Schema(implementation = TimezoneResponse::class))])])
  @Produces(MediaType.APPLICATION_JSON)
  fun getTimezones(): Response = TimezoneManager.getTimezonesResult().toResponse()

  @POST
  @Operation(
      summary = "Update system timezone.",
      security = [SecurityRequirement(name = "bearerAuth")],
      responses =
          [
              ApiResponse(
                  responseCode = "200",
                  description = "Updated system timezone successfully",
                  content =
                      [Content(schema = Schema(implementation = UpdateTimezoneResponse::class))])])
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  fun updateTimezone(request: UpdateTimezoneRequest): Response =
      TimezoneManager.updateTimezonesResult(request).toResponse()
}
