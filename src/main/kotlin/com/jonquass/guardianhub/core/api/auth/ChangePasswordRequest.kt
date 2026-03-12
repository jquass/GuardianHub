package com.jonquass.guardianhub.core.api.auth

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
    val serialNumber: String,
)
