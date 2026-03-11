package com.jonquass.guardianhub.core.api

data class StatusResponse(
    val status: String = "healthy",
    val timestamp: Long = System.currentTimeMillis(),
)
