package com.jonquass.guardianhub.resources

import com.jonquass.guardianhub.managers.ServiceStatusManager
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
        val status = ServiceStatusManager.getTaskStatus(taskId)

        return if (status != null) {
            Response
                .ok(
                    mapOf(
                        "status" to "success",
                        "task" to
                            mapOf(
                                "id" to status.taskId,
                                "status" to status.status,
                                "message" to status.message,
                                "progress" to status.progress,
                                "servicesRestarted" to status.servicesRestarted,
                                "servicesFailed" to status.servicesFailed,
                            ),
                    ),
                ).build()
        } else {
            Response
                .status(Response.Status.NOT_FOUND)
                .entity(
                    mapOf(
                        "status" to "error",
                        "message" to "Task not found",
                    ),
                ).build()
        }
    }
}
