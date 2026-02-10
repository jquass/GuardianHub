package com.jonquass.guardianhub.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import jakarta.ws.rs.ext.ContextResolver
import jakarta.ws.rs.ext.Provider

@Provider
class AppConfig : ContextResolver<ObjectMapper> {
    private val objectMapper = ObjectMapper().registerKotlinModule()

    override fun getContext(type: Class<*>?): ObjectMapper = objectMapper
}
