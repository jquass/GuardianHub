package com.jonquass.guardianhub.resource

import com.jonquass.guardianhub.core.api.ConfigResponse
import com.jonquass.guardianhub.core.toResponse
import com.jonquass.guardianhub.manager.ConfigManager
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/config")
class ConfigResource {
  @GET
  @Operation(
      summary = "Get Configuration",
      security = [SecurityRequirement(name = "bearerAuth")],
      responses =
          [
              ApiResponse(
                  responseCode = "200",
                  description =
                      "Returns all configurations in the .env file. It also masks sensitive fields.",
                  content = [Content(schema = Schema(implementation = ConfigResponse::class))])])
  @Produces(MediaType.APPLICATION_JSON)
  fun getConfig(): Response = ConfigManager.readConfig().toResponse()
}
