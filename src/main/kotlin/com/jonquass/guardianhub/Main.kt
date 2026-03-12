package com.jonquass.guardianhub

<<<<<<< Updated upstream
import com.jonquass.guardianhub.config.ServerFactory
import org.glassfish.grizzly.http.server.CLStaticHttpHandler
import org.glassfish.grizzly.http.server.StaticHttpHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun main() {
  val logger: Logger = LoggerFactory.getLogger("Main")
  val port = 8888
=======
import com.jonquass.guardianhub.config.ServerConfigFactory
import java.net.URI
import org.glassfish.grizzly.http.server.StaticHttpHandler
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory
import org.slf4j.LoggerFactory

fun main() {
  val logger = LoggerFactory.getLogger("Main")
  val port = System.getenv("PORT")?.toIntOrNull() ?: 8888
  val apiBaseUri = URI.create("http://0.0.0.0:$port/api/")
>>>>>>> Stashed changes

  logger.info("=== Guardian Hub ===")
  logger.info("Starting server on port {}", port)

<<<<<<< Updated upstream
  val server = ServerFactory.createHttpServer(port)

  // Existing static handler
=======
  val config = ServerConfigFactory.createResourceConfig()
  val server = GrizzlyHttpServerFactory.createHttpServer(apiBaseUri, config, false)

>>>>>>> Stashed changes
  val staticHandler = StaticHttpHandler("/app/static/")
  staticHandler.isFileCacheEnabled = false
  server.serverConfiguration.addHttpHandler(staticHandler, "/")

<<<<<<< Updated upstream
  // Add Swagger UI
  val swaggerHandler =
      CLStaticHttpHandler(
          object {}.javaClass.classLoader, "META-INF/resources/webjars/swagger-ui/5.17.14/")
  server.serverConfiguration.addHttpHandler(swaggerHandler, "/swagger-ui/")

  server.start()

  logger.info("Guardian Hub started successfully!")

  // Keep server running
=======
  server.start()

  logger.info("Serving static files from: /app/static/")
  logger.info("Guardian Hub Config UI started successfully!")
  logger.info("Web UI: http://localhost:{}/", port)
  logger.info("API Base: http://localhost:{}/api/", port)
  logger.info("Press Ctrl+C to stop the server")
>>>>>>> Stashed changes
  Thread.currentThread().join()
}