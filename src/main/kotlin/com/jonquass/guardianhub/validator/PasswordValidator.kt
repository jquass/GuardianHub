package com.jonquass.guardianhub.validator

import com.jonquass.guardianhub.core.Result
import jakarta.ws.rs.core.Response

object PasswordValidator {
  fun validate(password: String): Result<Unit> {
    if (password.isBlank()) {
      return Result.error("Password must not be blank", Response.Status.BAD_REQUEST)
    }

    if (password.length < 8) {
      return Result.error(
          "Password must be at least 8 characters long", Response.Status.BAD_REQUEST)
    }

    return Result.success()
  }
}
