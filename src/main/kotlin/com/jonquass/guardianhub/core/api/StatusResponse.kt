package com.jonquass.guardianhub.core.api

data class StatusResponse(
<<<<<<< Updated upstream
    val status: String = "healthy",
=======
    val status: String,
>>>>>>> Stashed changes
    val timestamp: Long = System.currentTimeMillis(),
)
