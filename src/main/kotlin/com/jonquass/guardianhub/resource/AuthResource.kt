package com.jonquass.guardianhub.resource

import com.jonquass.guardianhub.core.api.auth.ChangePasswordRequest
import com.jonquass.guardianhub.core.api.auth.CheckAuthResponse
import com.jonquass.guardianhub.core.api.auth.LoginRequest
import com.jonquass.guardianhub.core.api.auth.LoginResponse
import com.jonquass.guardianhub.core.api.auth.ResetToFactoryRequest
import com.jonquass.guardianhub.core.api.auth.ResetToFactoryResponse
import com.jonquass.guardianhub.core.toResponse
import com.jonquass.guardianhub.manager.auth.AuthManager
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
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
  @Operation(
      summary = "Login",
      responses =
          [
              ApiResponse(
                  responseCode = "200",
                  description = "Login successful",
                  content = [Content(schema = Schema(implementation = LoginResponse::class))])])
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  fun login(request: LoginRequest): Response = AuthManager.login(request).toResponse()

  @POST
  @Path("/reset-to-factory")
  @Operation(
      summary = "Reset Login Password to Factory Default",
      responses =
          [
              ApiResponse(
                  responseCode = "200",
                  description = "Reset factory password successfully",
                  content =
                      [Content(schema = Schema(implementation = ResetToFactoryResponse::class))])])
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  fun resetToFactory(request: ResetToFactoryRequest): Response =
      AuthManager.resetToFactory(request).toResponse()

  @POST
  @Path("/change-password")
  @Operation(
      summary = "Change Login Password",
      security = [SecurityRequirement(name = "bearerAuth")],
      responses =
          [
              ApiResponse(
                  responseCode = "200",
                  description = "Login password changed successfully",
                  content =
                      [Content(schema = Schema(implementation = ResetToFactoryResponse::class))])])
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  fun changePassword(
      @HeaderParam("Authorization") authHeader: String?,
      request: ChangePasswordRequest,
  ): Response = AuthManager.changePassword(authHeader, request).toResponse()

  @POST
  @Path("/logout")
  @Operation(
      summary = "Logout",
      responses =
          [
              ApiResponse(
                  responseCode = "200",
                  description = "Logged out successfully",
                  content = [Content(schema = Schema(implementation = Unit::class))])])
  @Produces(MediaType.APPLICATION_JSON)
  fun logout(
      @HeaderParam("Authorization") authHeader: String?,
  ): Response = AuthManager.logout(authHeader).toResponse()

  @GET
  @Path("/check")
  @Operation(
      summary = "Check Auth Token",
      responses =
          [
              ApiResponse(
                  responseCode = "200",
                  description = "Auth token checked successfully",
                  content = [Content(schema = Schema(implementation = CheckAuthResponse::class))])])
  @Produces(MediaType.APPLICATION_JSON)
  fun checkAuth(
      @HeaderParam("Authorization") authHeader: String?,
  ): Response = AuthManager.checkAuth(authHeader).toResponse()
}
