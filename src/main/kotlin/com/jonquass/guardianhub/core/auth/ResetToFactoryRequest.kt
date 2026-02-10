package com.jonquass.guardianhub.core.auth

data class ResetToFactoryRequest(
    val factoryPassword: String,
    val serialNumber: String,
)
