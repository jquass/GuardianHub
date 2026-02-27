package com.jonquass.guardianhub.core.exception

import jakarta.ws.rs.core.Response

class ResultException(
    message: String,
    val code: Response.Status = Response.Status.INTERNAL_SERVER_ERROR,
) : RuntimeException(message)
