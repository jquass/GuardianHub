package com.jonquass.guardianhub.core.api

data class UpdatePasswordResponse(
    val status: String,
    val message: String,
    val serviceRestarted: Boolean,
)
