package com.jonquass.guardianhub.validator

import com.jonquass.guardianhub.core.validator.PasswordResult
import jakarta.ws.rs.core.Response

object PasswordValidator {
    fun validate(password: String): PasswordResult {
        if (password.isBlank()) {
            return PasswordResult(errorResponse("Password cannot be empty"))
        }

        if (password.length < 8) {
            return PasswordResult(errorResponse("Password must be at least 8 characters"))
        }

        return PasswordResult()
    }

    private fun errorResponse(message: String): Response =
        Response
            .status(Response.Status.BAD_REQUEST)
            .entity(
                mapOf(
                    "status" to "error",
                    "message" to message,
                ),
            ).build()
}
