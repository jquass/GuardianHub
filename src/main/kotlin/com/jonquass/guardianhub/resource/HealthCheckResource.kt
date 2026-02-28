package com.jonquass.guardianhub.resource

import com.jonquass.guardianhub.core.api.StatusResponse
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/")
class HealthCheckResource {
  @GET
  @Path("/health")
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
