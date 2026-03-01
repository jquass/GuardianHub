package com.jonquass.guardianhub

import com.jonquass.guardianhub.config.ServerFactory
import com.jonquass.guardianhub.core.config.Env
import com.jonquass.guardianhub.manager.ConfigManager
import com.jonquass.guardianhub.manager.auth.AuthManager
import com.jonquass.guardianhub.manager.auth.PasswordHashManager
import com.jonquass.guardianhub.manager.auth.SessionManager
import io.restassured.RestAssured
import java.io.File
import java.time.ZoneId
import org.glassfish.grizzly.http.server.HttpServer
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class GrizzlyServerExtension :
    BeforeAllCallback, BeforeEachCallback, ExtensionContext.Store.CloseableResource {

  companion object {
    private var started = false
    private var server: HttpServer? = null
    private const val PORT = 9998

    internal const val TEST_PASSWORD = "password123"
    internal const val TEST_SERIAL_NUMBER = "serial123"

    private lateinit var configFile: File
    private lateinit var factoryPasswordFile: File
    private lateinit var serialNumberFile: File
  }

  override fun beforeAll(context: ExtensionContext) {
    if (!started) {
      started = true

      configFile = File.createTempFile("test-env", ".env")
      factoryPasswordFile = File.createTempFile("test-factory", ".factory-password")
      serialNumberFile = File.createTempFile("test-serial", ".serial-number")

      writeTestFixtures()

      ConfigManager.configFile = configFile
      AuthManager.factoryPasswordFile = factoryPasswordFile
      AuthManager.serialNumberFile = serialNumberFile

      server = ServerFactory.createHttpServer(PORT)
      server?.start()

      RestAssured.baseURI = ServerFactory.API_BASE
      RestAssured.port = PORT

      context.root.getStore(ExtensionContext.Namespace.GLOBAL).put("grizzly-server", this)
    }
  }

  override fun beforeEach(context: ExtensionContext) {
    // Reset to known state before each test
    writeTestFixtures()
    SessionManager.invalidateSessions()
  }

  override fun close() {
    server?.shutdownNow()
    configFile.delete()
    factoryPasswordFile.delete()
    serialNumberFile.delete()
    ConfigManager.configFile = File(ConfigManager.DEFAULT_CONFIG_PATH)
    AuthManager.factoryPasswordFile = File(AuthManager.DEFAULT_FACTORY_PASSWORD_PATH)
    AuthManager.serialNumberFile = File(AuthManager.DEFAULT_SERIAL_NUMBER_PATH)
  }

  private fun writeTestFixtures() {
    val passwordHash = PasswordHashManager.hashPassword(TEST_PASSWORD)
    configFile.writeText(
        """
      ${Env.LOGIN_PASSWORD}='$passwordHash'
      ${Env.TZ}='${ZoneId.systemDefault()}'
      """
            .trimIndent())
    factoryPasswordFile.writeText(passwordHash)
    serialNumberFile.writeText(PasswordHashManager.hashPassword(TEST_SERIAL_NUMBER))
  }
}
