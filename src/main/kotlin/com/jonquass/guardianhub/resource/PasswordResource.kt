package com.jonquass.guardianhub.resource

import com.jonquass.guardianhub.core.api.UpdatePasswordRequest
import com.jonquass.guardianhub.core.api.UpdatePasswordResponse
import com.jonquass.guardianhub.core.toResponse
import com.jonquass.guardianhub.manager.PasswordManager
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
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
  @Operation(
      summary = "Change Pihole Password",
      security = [SecurityRequirement(name = "bearerAuth")],
      responses =
          [
              ApiResponse(
                  responseCode = "200",
                  description = "Pihole Password Changed Successfully",
                  content =
                      [Content(schema = Schema(implementation = UpdatePasswordResponse::class))])])
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  fun updatePiholePassword(request: UpdatePasswordRequest): Response =
      PasswordManager.updatePiholePassword(request).toResponse()

  @POST
  @Path("/wireguard")
  @Operation(
      summary = "Change WireGuard Password",
      security = [SecurityRequirement(name = "bearerAuth")],
      responses =
          [
              ApiResponse(
                  responseCode = "200",
                  description = "WireGuard Password Changed Successfully",
                  content =
                      [Content(schema = Schema(implementation = UpdatePasswordResponse::class))])])
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  fun updateWireGuardPassword(request: UpdatePasswordRequest): Response =
      PasswordManager.updateWireGuardPassword(request).toResponse()

  @POST
  @Path("/npm")
  @Operation(
      summary = "Change NPM Password",
      security = [SecurityRequirement(name = "bearerAuth")],
      responses =
          [
              ApiResponse(
                  responseCode = "200",
                  description = "NPM Password Changed Successfully",
                  content =
                      [Content(schema = Schema(implementation = UpdatePasswordResponse::class))])])
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  fun updateNpmPassword(request: UpdatePasswordRequest): Response =
      PasswordManager.updateNpmPassword(request).toResponse()
}
