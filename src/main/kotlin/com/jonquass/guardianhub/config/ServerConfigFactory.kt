package com.jonquass.guardianhub.config

import org.glassfish.jersey.server.ResourceConfig

object ServerConfigFactory {
    fun createResourceConfig(includeAppConfig: Boolean = true): ResourceConfig {
        val config = ResourceConfig()
            .packages("com.jonquass.guardianhub.resources")
            .packages("com.jonquass.guardianhub.filters")

        if (includeAppConfig) {
            config.register(AppConfig::class.java)
        }

        return config
    }
}