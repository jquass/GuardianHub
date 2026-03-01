package com.jonquass.guardianhub

import com.jonquass.guardianhub.config.ServerFactory
import io.restassured.RestAssured
import org.glassfish.grizzly.http.server.HttpServer
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext

class GrizzlyServerExtension : BeforeAllCallback, ExtensionContext.Store.CloseableResource {

  companion object {
    private var started = false
    private lateinit var server: HttpServer
    const val PORT = 9998
  }

  override fun beforeAll(context: ExtensionContext) {
    if (!started) {
      started = true

      server = ServerFactory.createHttpServer(PORT)
      server.start()

      RestAssured.baseURI = ServerFactory.API_BASE
      RestAssured.port = PORT

      context.root.getStore(ExtensionContext.Namespace.GLOBAL).put("grizzly-server", this)
    }
  }

  override fun close() {
    server.shutdownNow()
  }
}
