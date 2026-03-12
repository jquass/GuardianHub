package com.jonquass.guardianhub.core.api.auth

data class ResetToFactoryRequest(
    val factoryPassword: String,
    val serialNumber: String,
)
