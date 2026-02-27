package com.jonquass.guardianhub

import java.net.URI
import org.glassfish.grizzly.http.server.HttpServer
import org.glassfish.grizzly.http.server.StaticHttpHandler
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory
import org.glassfish.jersey.server.ResourceConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main() {
  val logger: Logger = LoggerFactory.getLogger("Main")
  val port = System.getenv("PORT")?.toIntOrNull() ?: 8888
  val apiBaseUri = URI.create("http://0.0.0.0:$port/api/")

  logger.info("=== Guardian Hub Config UI ===")
  logger.info("Starting server on port {}", port)

  // Create resource config
  val config =
      ResourceConfig()
          .packages("com.jonquass.guardianhub.resources")
          .packages("com.jonquass.guardianhub.filters")
          .register(com.jonquass.guardianhub.config.AppConfig::class.java)

  // Create server with API base at /api/
  val server: HttpServer = GrizzlyHttpServerFactory.createHttpServer(apiBaseUri, config, false)

  // Add static file handler at root (before starting server)
  val staticHandler = StaticHttpHandler("/app/static/")
  staticHandler.isFileCacheEnabled = false
  server.serverConfiguration.addHttpHandler(staticHandler, "/")

  // Start the server
  server.start()

  logger.info("Serving static files from: /app/static/")
  logger.info("Guardian Hub Config UI started successfully!")
  logger.info("Web UI: http://localhost:{}/", port)
  logger.info("API Base: http://localhost:{}/api/", port)
  logger.info("Press Ctrl+C to stop the server")

  // Keep server running
  Thread.currentThread().join()
}
