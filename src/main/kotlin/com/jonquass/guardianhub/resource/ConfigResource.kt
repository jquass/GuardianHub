package com.jonquass.guardianhub.resource

import com.jonquass.guardianhub.core.manager.toResponse
import com.jonquass.guardianhub.manager.ConfigManager
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/config")
class ConfigResource {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun getConfig(): Response = ConfigManager.readConfig().toResponse()
}
