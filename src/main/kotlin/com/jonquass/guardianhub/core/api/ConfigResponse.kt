package com.jonquass.guardianhub.core.api

import com.jonquass.guardianhub.core.config.CategoryInfo
import com.jonquass.guardianhub.core.config.ConfigEntry

data class ConfigResponse(
    val categories: List<CategoryInfo>,
    val entries: List<ConfigEntry>,
)
