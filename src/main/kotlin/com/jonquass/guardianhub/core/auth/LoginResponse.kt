package com.jonquass.guardianhub.core.auth

data class LoginResponse(
    val success: Boolean,
    val token: String? = null,
    val message: String? = null,
)
