package com.jonquass.guardianhub.core.config

data class ConfigEntry(
    val key: String,
    val value: String,
    val categoryName: String,
    val description: String,
    val sensitive: Boolean,
    val tooltip: String?,
)
