package com.jonquass.guardianhub.core.api

data class UpdateTimezoneResponse(
    val status: String,
    val message: String,
    val servicesRestarted: List<String>,
)
