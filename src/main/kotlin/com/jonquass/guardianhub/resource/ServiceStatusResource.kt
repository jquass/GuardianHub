package com.jonquass.guardianhub.resource

import com.jonquass.guardianhub.core.toResponse
import com.jonquass.guardianhub.manager.ServiceStatusManager
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/status")
class ServiceStatusResource {
  @GET
  @Path("/task/{taskId}")
  @Produces(MediaType.APPLICATION_JSON)
  fun getTaskStatus(
      @PathParam("taskId") taskId: String,
  ): Response {
    return ServiceStatusManager.getTaskStatus(taskId).toResponse()
  }
}
