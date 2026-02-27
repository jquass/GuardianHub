package com.jonquass.guardianhub.resource

import com.jonquass.guardianhub.core.auth.ChangePasswordRequest
import com.jonquass.guardianhub.core.auth.LoginRequest
import com.jonquass.guardianhub.core.auth.ResetToFactoryRequest
import com.jonquass.guardianhub.core.manager.toResponse
import com.jonquass.guardianhub.manager.auth.AuthManager
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/auth")
class AuthResource {
  @POST
  @Path("/login")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  fun login(request: LoginRequest): Response = AuthManager.login(request).toResponse()

  @POST
  @Path("/reset-to-factory")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  fun resetToFactory(request: ResetToFactoryRequest): Response =
      AuthManager.resetToFactory(request).toResponse()

  @POST
  @Path("/change-password")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  fun changePassword(
      @HeaderParam("Authorization") authHeader: String?,
      request: ChangePasswordRequest,
  ): Response = AuthManager.changePassword(authHeader, request).toResponse()

  @POST
  @Path("/logout")
  @Produces(MediaType.APPLICATION_JSON)
  fun logout(
      @HeaderParam("Authorization") authHeader: String?,
  ): Response = AuthManager.logout(authHeader).toResponse()

  @GET
  @Path("/check")
  @Produces(MediaType.APPLICATION_JSON)
  fun checkAuth(
      @HeaderParam("Authorization") authHeader: String?,
  ): Response = AuthManager.checkAuth(authHeader).toResponse()
}
