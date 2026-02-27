package com.jonquass.guardianhub.resource

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

data class StatusResponse(
    val status: String,
    val service: String,
    val version: String,
    val timestamp: Long = System.currentTimeMillis(),
)

@Path("/")
class HealthCheckResource {
  @GET
  @Path("/status")
  @Produces(MediaType.APPLICATION_JSON)
  fun status(): Response {
    val response =
        StatusResponse(
            status = "healthy",
            service = "Guardian Hub Config UI",
            version = "1.0.0",
        )
    return Response.ok(response).build()
  }
}
