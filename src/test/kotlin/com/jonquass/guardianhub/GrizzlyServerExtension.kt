package com.jonquass.guardianhub

import com.jonquass.guardianhub.config.ServerFactory
import com.jonquass.guardianhub.core.config.Env
import com.jonquass.guardianhub.manager.ConfigManager
import com.jonquass.guardianhub.manager.DockerManager
import com.jonquass.guardianhub.manager.auth.AuthManager
import com.jonquass.guardianhub.manager.auth.PasswordHashManager
import com.jonquass.guardianhub.manager.auth.SessionManager
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import jakarta.ws.rs.core.Response
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

    fun loginAndGetToken(password: String = TEST_PASSWORD): String {
      return Given {
        contentType(ContentType.JSON)
        body("""{"password": "$password"}""")
      } When
          {
            post("/api/auth/login")
          } Then
          {
            statusCode(Response.Status.OK.statusCode)
          } Extract
          {
            path("token")
          }
    }
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
    mockkObject(DockerManager)
    every { DockerManager.exec(*anyVararg<String>()) } returns true
    every { DockerManager.recreateContainer(any()) } returns true
    every { DockerManager.execWithOutput(*anyVararg()) } answers
        {
          val args = args.first() as Array<*>
          when {
            // NPM token fetch
            args.any { it.toString().contains("/api/tokens") } ->
                Pair(0, """{"token":"mock-npm-token","expires":"2025-01-01"}""")
            // NPM user list
            args.any { it.toString().contains("/api/users") && !it.toString().contains("/auth") } ->
                Pair(0, """[{"id":1,"email":"admin@example.com"}]""")
            // NPM password update
            args.any { it.toString().contains("/auth") } -> Pair(0, "true")
            // WireGuard password hash
            args.any { it.toString().contains("wgpw") } ->
                Pair(0, "PASSWORD_HASH=\$2a\$10\$mockedhash")
            // Default
            else -> Pair(0, "ok")
          }
        }
  }

  override fun close() {
    unmockkAll()
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
      ${Env.NPM_ADMIN_EMAIL}=admin@example.com
      ${Env.NPM_ADMIN_PASSWORD}=oldpassword
      """
            .trimIndent())
    factoryPasswordFile.writeText(passwordHash)
    serialNumberFile.writeText(PasswordHashManager.hashPassword(TEST_SERIAL_NUMBER))
  }
}
