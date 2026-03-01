package com.jonquass.guardianhub.config

import java.net.URI
import org.glassfish.grizzly.http.server.HttpServer
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory
import org.glassfish.jersey.server.ResourceConfig

object ServerFactory {

  const val API_BASE = "http://0.0.0.0"
  const val API_URL_TEMPLATE = "${API_BASE}:%d/api/"

  fun createHttpServer(port: Int): HttpServer {
    val config =
        ResourceConfig()
            .packages("com.jonquass.guardianhub.resource")
            .packages("com.jonquass.guardianhub.filter")
            .register(AppConfig::class.java)
    val uri = URI.create(API_URL_TEMPLATE.format(port))
    return GrizzlyHttpServerFactory.createHttpServer(uri, config, false)
  }
}
