package com.jonquass.guardianhub.resources

import com.jonquass.guardianhub.core.auth.ChangePasswordRequest
import com.jonquass.guardianhub.core.auth.LoginRequest
import com.jonquass.guardianhub.core.auth.LoginResponse
import com.jonquass.guardianhub.core.auth.ResetToFactoryRequest
import com.jonquass.guardianhub.managers.AuthManager
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
    fun login(request: LoginRequest): Response {
        val isValid = AuthManager.validatePassword(request.password)

        if (isValid) {
            val token = AuthManager.createSession()
            return Response
                .ok(
                    LoginResponse(
                        success = true,
                        token = token,
                    ),
                ).build()
        } else {
            return Response
                .status(Response.Status.UNAUTHORIZED)
                .entity(
                    LoginResponse(
                        success = false,
                        message = "Invalid password",
                    ),
                ).build()
        }
    }

    @POST
    @Path("/reset-to-factory")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun resetToFactory(request: ResetToFactoryRequest): Response {
        val success =
            AuthManager.resetToFactoryPassword(
                request.factoryPassword,
                request.serialNumber,
            )

        if (success) {
            return Response
                .ok(
                    mapOf(
                        "success" to true,
                        "message" to "Password reset to factory default",
                    ),
                ).build()
        } else {
            return Response
                .status(Response.Status.BAD_REQUEST)
                .entity(
                    mapOf(
                        "success" to false,
                        "message" to "Invalid factory password or serial number",
                    ),
                ).build()
        }
    }

    @POST
    @Path("/change-password")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun changePassword(
        @HeaderParam("Authorization") authHeader: String?,
        request: ChangePasswordRequest,
    ): Response {
        // Verify user is authenticated
        val token = authHeader?.removePrefix("Bearer ")
        if (token == null || !AuthManager.isValidSession(token)) {
            return Response
                .status(Response.Status.UNAUTHORIZED)
                .entity(mapOf("success" to false, "message" to "Unauthorized"))
                .build()
        }

        val success =
            AuthManager.changeLoginPassword(
                request.currentPassword,
                request.newPassword,
                request.serialNumber,
            )

        if (success) {
            return Response
                .ok(
                    mapOf(
                        "success" to true,
                        "message" to "Login password changed successfully",
                    ),
                ).build()
        } else {
            return Response
                .status(Response.Status.BAD_REQUEST)
                .entity(
                    mapOf(
                        "success" to false,
                        "message" to
                            "Failed to change password. Check current password, serial number, and ensure new password is at least 8 characters.",
                    ),
                ).build()
        }
    }

    @POST
    @Path("/logout")
    @Produces(MediaType.APPLICATION_JSON)
    fun logout(
        @HeaderParam("Authorization") authHeader: String?,
    ): Response {
        val token = authHeader?.removePrefix("Bearer ")
        if (token != null) {
            AuthManager.invalidateSession(token)
        }
        return Response.ok(mapOf("success" to true)).build()
    }

    @GET
    @Path("/check")
    @Produces(MediaType.APPLICATION_JSON)
    fun checkAuth(
        @HeaderParam("Authorization") authHeader: String?,
    ): Response {
        val token = authHeader?.removePrefix("Bearer ")
        val isValid = token?.let { AuthManager.isValidSession(it) } ?: false

        return Response.ok(mapOf("authenticated" to isValid)).build()
    }
}
