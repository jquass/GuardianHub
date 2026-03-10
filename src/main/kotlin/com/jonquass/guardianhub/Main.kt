package com.jonquass.guardianhub

import com.jonquass.guardianhub.config.ServerFactory
import org.glassfish.grizzly.http.server.CLStaticHttpHandler
import org.glassfish.grizzly.http.server.StaticHttpHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main() {
  val logger: Logger = LoggerFactory.getLogger("Main")
  val port = 8888

  logger.info("=== Guardian Hub ===")
  logger.info("Starting server on port {}", port)

  val server = ServerFactory.createHttpServer(port)

  // Existing static handler
  val staticHandler = StaticHttpHandler("/app/static/")
  staticHandler.isFileCacheEnabled = false
  server.serverConfiguration.addHttpHandler(staticHandler, "/")

  // Add Swagger UI
  val swaggerHandler =
      CLStaticHttpHandler(
          object {}.javaClass.classLoader, "META-INF/resources/webjars/swagger-ui/5.17.14/")
  server.serverConfiguration.addHttpHandler(swaggerHandler, "/swagger-ui/")

  server.start()

  logger.info("Guardian Hub started successfully!")

  // Keep server running
  Thread.currentThread().join()
}
