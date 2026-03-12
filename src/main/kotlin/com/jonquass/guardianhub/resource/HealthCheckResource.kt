package com.jonquass.guardianhub.resource

import com.jonquass.guardianhub.core.api.StatusResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/")
class HealthCheckResource {
  @GET
  @Path("/health")
  @Operation(
      summary = "Service Healthcheck",
      responses =
          [
              ApiResponse(
                  responseCode = "200",
                  description = "Service is healthy",
                  content = [Content(schema = Schema(implementation = StatusResponse::class))])])
  @Produces(MediaType.APPLICATION_JSON)
  fun status(): Response {
<<<<<<< Updated upstream
    return Response.ok(StatusResponse()).build()
=======
    val response =
        StatusResponse(
            status = "healthy",
        )
    return Response.ok(response).build()
>>>>>>> Stashed changes
  }
}
