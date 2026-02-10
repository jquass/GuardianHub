package com.jonquass.guardianhub.core.validators

import jakarta.ws.rs.core.Response

data class PasswordResult(
    val errorResponse: Response? = null,
)
