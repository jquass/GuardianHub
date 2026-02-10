package com.jonquass.guardianhub.core.auth

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
    val serialNumber: String,
)
