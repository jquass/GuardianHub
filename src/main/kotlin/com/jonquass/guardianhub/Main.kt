package com.jonquass.guardianhub

import com.jonquass.guardianhub.config.ServerFactory
import org.glassfish.grizzly.http.server.StaticHttpHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main() {
  val logger: Logger = LoggerFactory.getLogger("Main")
  val port = 8888

  logger.info("=== Guardian Hub ===")
  logger.info("Starting server on port {}", port)

  val server = ServerFactory.createHttpServer(port)
  val staticHandler = StaticHttpHandler("/app/static/")
  staticHandler.isFileCacheEnabled = false
  server.serverConfiguration.addHttpHandler(staticHandler, "/")
  server.start()

  logger.info("Guardian Hub started successfully!")

  // Keep server running
  Thread.currentThread().join()
}
