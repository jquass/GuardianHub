package com.jonquass.guardianhub.core.validator

import jakarta.ws.rs.core.Response

data class PasswordResult(
    val errorResponse: Response? = null,
)
