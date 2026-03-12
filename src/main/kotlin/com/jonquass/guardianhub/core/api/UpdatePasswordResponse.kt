package com.jonquass.guardianhub.core.api

data class UpdatePasswordResponse(
    val message: String,
    val serviceRestarted: Boolean,
)
