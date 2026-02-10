package com.jonquass.guardianhub.resources

import com.jonquass.guardianhub.core.api.UpdatePasswordRequest
import com.jonquass.guardianhub.managers.PasswordManager
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/password")
class PasswordResource {
    @POST
    @Path("/pihole")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun updatePiholePassword(request: UpdatePasswordRequest): Response = PasswordManager.updatePiholePassword(request)

    @POST
    @Path("/wireguard")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun updateWireGuardPassword(request: UpdatePasswordRequest): Response = PasswordManager.updateWireGuardPassword(request)

    @POST
    @Path("/npm")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun updateNpmPassword(request: UpdatePasswordRequest): Response = PasswordManager.updateNpmPassword(request)
}
