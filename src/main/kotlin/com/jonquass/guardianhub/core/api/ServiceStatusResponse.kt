package com.jonquass.guardianhub.core.api

data class ServiceStatusResponse(
    val taskId: String,
    val status: String, // "pending", "running", "completed", "failed"
    val message: String,
    val progress: Int, // 0-100
    val servicesRestarted: List<String> = emptyList(),
    val servicesFailed: List<String> = emptyList(),
)
