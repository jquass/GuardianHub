package com.jonquass.guardianhub.resource

import com.jonquass.guardianhub.core.api.ServiceStatusResponse
import com.jonquass.guardianhub.core.toResponse
import com.jonquass.guardianhub.manager.ServiceStatusManager
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
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
  @Operation(
      summary =
          "Get the status of a task. Used for UI status updates during long running Docker operations.",
      security = [SecurityRequirement(name = "bearerAuth")],
      responses =
          [
              ApiResponse(
                  responseCode = "200",
                  description = "Fetched task status successfully",
                  content =
                      [
                          Content(
                              schema =
                                  Schema(
                                      implementation = ServiceStatusResponse::class,
                                      nullable = true))])])
  @Produces(MediaType.APPLICATION_JSON)
  fun getTaskStatus(
      @PathParam("taskId") taskId: String,
  ): Response {
    return ServiceStatusManager.getTaskStatus(taskId).toResponse()
  }
}
