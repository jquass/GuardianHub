package com.jonquass.guardianhub

import io.restassured.RestAssured
import java.net.URI
import org.glassfish.grizzly.http.server.HttpServer
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory
import org.glassfish.jersey.server.ResourceConfig
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext

class GrizzlyServerExtension : BeforeAllCallback, ExtensionContext.Store.CloseableResource {

  companion object {
    private var started = false
    private lateinit var server: HttpServer
    const val BASE_URI = "http://localhost"
    const val PORT = 9998
  }

  override fun beforeAll(context: ExtensionContext) {
    if (!started) {
      started = true

      val config =
          ResourceConfig().apply {
            packages("com.jonquass.guardianhub.resource")
            packages("com.jonquass.guardianhub.filters")
          }

      server = GrizzlyHttpServerFactory.createHttpServer(URI.create("$BASE_URI:$PORT/"), config)
      server.start()

      RestAssured.baseURI = BASE_URI
      RestAssured.port = PORT

      // Register for cleanup when the root test context closes (end of suite)
      context.root.getStore(ExtensionContext.Namespace.GLOBAL).put("grizzly-server", this)
    }
  }

  override fun close() {
      server.shutdownNow()
  }
}
