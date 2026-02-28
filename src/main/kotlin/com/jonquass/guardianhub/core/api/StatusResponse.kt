package com.jonquass.guardianhub.core.api

data class StatusResponse(
    val status: String,
    val service: String,
    val version: String,
    val timestamp: Long = System.currentTimeMillis(),
)
