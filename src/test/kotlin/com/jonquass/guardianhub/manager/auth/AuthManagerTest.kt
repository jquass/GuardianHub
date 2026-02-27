package com.jonquass.guardianhub.manager.auth

import com.jonquass.guardianhub.core.auth.ChangePasswordRequest
import com.jonquass.guardianhub.core.auth.LoginRequest
import com.jonquass.guardianhub.core.auth.ResetToFactoryRequest
import com.jonquass.guardianhub.core.config.Env
import com.jonquass.guardianhub.core.errOrThrow
import com.jonquass.guardianhub.core.getOrThrow
import com.jonquass.guardianhub.manager.ConfigManager
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import jakarta.ws.rs.core.Response
import java.io.File
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.MapEntry.entry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthManagerTest {
  private val password = "MySecurePassword"
  private val serialNumber = "MySerialNumber"

  private lateinit var configFile: File
  private lateinit var factoryPasswordFile: File
  private lateinit var serialNumberFile: File

  @BeforeEach
  fun setUp() {
    configFile = File.createTempFile("test-env", ".env")
    factoryPasswordFile = File.createTempFile("test-factory-password", ".factory-password")
    serialNumberFile = File.createTempFile("test-serial-number", ".serial-number")

    ConfigManager.configFile = configFile
    AuthManager.factoryPasswordFile = factoryPasswordFile
    AuthManager.serialNumberFile = serialNumberFile

    // Write a default login password hash to the config file
    val passwordHash = PasswordHashManager.hashPassword(password)
    configFile.writeText("LOGIN_PASSWORD='$passwordHash'\n")

    // Write a default factory password hash
    factoryPasswordFile.writeText(PasswordHashManager.hashPassword(password))

    // Write a default serial number hash
    serialNumberFile.writeText(PasswordHashManager.hashPassword(serialNumber))
  }

  @AfterEach
  fun tearDown() {
    configFile.delete()
    factoryPasswordFile.delete()
    serialNumberFile.delete()
    ConfigManager.configFile = File(ConfigManager.DEFAULT_CONFIG_PATH)
    AuthManager.factoryPasswordFile = File(AuthManager.DEFAULT_FACTORY_PASSWORD_PATH)
    AuthManager.serialNumberFile = File(AuthManager.DEFAULT_SERIAL_NUMBER_PATH)
    SessionManager.invalidateSessions()
    unmockkAll()
  }

  @Test
  fun `login should return token on success`() {
    val result = AuthManager.login(LoginRequest(password))

    assertTrue(result.isSuccess)
    assertNotNull(result.getOrThrow().token)
  }

  @Test
  fun `login should fail with incorrect password`() {
    val result = AuthManager.login(LoginRequest("WrongPassword"))

    assertTrue(result.isError)
  }

  @Test
  fun `login should fail when no password hash in config`() {
    configFile.writeText("")

    val result = AuthManager.login(LoginRequest(password))

    assertTrue(result.isError)
  }

  @Test
  fun `logout should succeed and invalidate session`() {
    val loginResult = AuthManager.login(LoginRequest(password))
    assertTrue(loginResult.isSuccess)
    val token = loginResult.getOrThrow().token

    AuthManager.logout("Bearer $token")

    val checkResult = AuthManager.checkAuth("Bearer $token")
    assertTrue(checkResult.isSuccess)
    val entries = checkResult.getOrThrow().entries
    assertThat(entries).containsExactly(entry("authenticated", false))
  }

  @Test
  fun `logout should succeed even with null auth header`() {
    val result = AuthManager.logout(null)

    assertTrue(result.isSuccess)
  }

  @Test
  fun `checkAuth should return true for valid session`() {
    val loginResult = AuthManager.login(LoginRequest(password))
    assertTrue(loginResult.isSuccess)
    val token = loginResult.getOrThrow().token

    val checkResult = AuthManager.checkAuth("Bearer $token")

    assertTrue(checkResult.isSuccess)
    val entries = checkResult.getOrThrow().entries
    assertThat(entries).containsExactly(entry("authenticated", true))
  }

  @Test
  fun `checkAuth should return false for invalid token`() {
    val checkResult = AuthManager.checkAuth("Bearer invalid-token")

    assertTrue(checkResult.isSuccess)
    val entries = checkResult.getOrThrow().entries
    assertThat(entries).containsExactly(entry("authenticated", false))
  }

  @Test
  fun `checkAuth should return false for null auth header`() {
    val checkResult = AuthManager.checkAuth(null)

    assertTrue(checkResult.isSuccess)
    val entries = checkResult.getOrThrow().entries
    assertThat(entries).containsExactly(entry("authenticated", false))
  }

  @Test
  fun `changePassword should succeed with correct current password and serial number`() {
    val loginResult = AuthManager.login(LoginRequest(password))
    assertTrue(loginResult.isSuccess)
    val token = loginResult.getOrThrow().token

    val result =
        AuthManager.changePassword(
            "Bearer $token",
            ChangePasswordRequest(password, "NewPassword123", serialNumber),
        )

    assertTrue(result.isSuccess)
  }

  @Test
  fun `changePassword should allow login with new password after change`() {
    val loginResult = AuthManager.login(LoginRequest(password))
    assertTrue(loginResult.isSuccess)
    val token = loginResult.getOrThrow().token
    val newPassword = "NewPassword123"

    AuthManager.changePassword(
        "Bearer $token",
        ChangePasswordRequest(password, newPassword, serialNumber),
    )

    val newLoginResult = AuthManager.login(LoginRequest(newPassword))
    assertTrue(newLoginResult.isSuccess)
  }

  @Test
  fun `changePassword should fail with incorrect current password`() {
    val loginResult = AuthManager.login(LoginRequest(password))
    assertTrue(loginResult.isSuccess)
    val token = loginResult.getOrThrow().token

    val result =
        AuthManager.changePassword(
            "Bearer $token",
            ChangePasswordRequest("WrongPassword", "NewPassword123", serialNumber),
        )

    assertTrue(result.isError)
    assertThat(result.errOrThrow().code).isEqualTo(Response.Status.BAD_REQUEST)
  }

  @Test
  fun `changePassword should fail with incorrect serial number`() {
    val loginResult = AuthManager.login(LoginRequest(password))
    assertTrue(loginResult.isSuccess)
    val token = loginResult.getOrThrow().token

    val result =
        AuthManager.changePassword(
            "Bearer $token",
            ChangePasswordRequest(password, "NewPassword123", "WrongSerial"),
        )

    assertTrue(result.isError)
    assertThat(result.errOrThrow().code).isEqualTo(Response.Status.BAD_REQUEST)
  }

  @Test
  fun `changePassword should fail when new password is less than 8 characters`() {
    val loginResult = AuthManager.login(LoginRequest(password))
    assertTrue(loginResult.isSuccess)
    val token = loginResult.getOrThrow().token

    val result =
        AuthManager.changePassword(
            "Bearer $token",
            ChangePasswordRequest(password, "short", serialNumber),
        )

    assertTrue(result.isError)
    assertThat(result.errOrThrow().code).isEqualTo(Response.Status.BAD_REQUEST)
  }

  @Test
  fun `changePassword should fail with invalid auth token`() {
    val result =
        AuthManager.changePassword(
            "Bearer invalid-token",
            ChangePasswordRequest(password, "NewPassword123", serialNumber),
        )

    assertTrue(result.isError)
    assertThat(result.errOrThrow().code).isEqualTo(Response.Status.UNAUTHORIZED)
  }

  @Test
  fun `resetToFactory should succeed with correct factory password and serial number`() {
    val result =
        AuthManager.resetToFactory(
            ResetToFactoryRequest(password, serialNumber),
        )

    assertTrue(result.isSuccess)
  }

  @Test
  fun `resetToFactory should allow login with factory password after reset`() {
    val newPassword = "NewPassword123"
    val loginResult = AuthManager.login(LoginRequest(password))
    assertTrue(loginResult.isSuccess)
    val token = loginResult.getOrThrow().token
    AuthManager.changePassword(
        "Bearer $token",
        ChangePasswordRequest(password, newPassword, serialNumber),
    )

    AuthManager.resetToFactory(ResetToFactoryRequest(password, serialNumber))

    val resetLoginResult = AuthManager.login(LoginRequest(password))
    assertTrue(resetLoginResult.isSuccess)
  }

  @Test
  fun `resetToFactory should invalidate all sessions`() {
    val loginResult = AuthManager.login(LoginRequest(password))
    assertTrue(loginResult.isSuccess)
    val token = loginResult.getOrThrow().token

    AuthManager.resetToFactory(ResetToFactoryRequest(password, serialNumber))

    val checkResult = AuthManager.checkAuth("Bearer $token")
    assertTrue(checkResult.isSuccess)
    val entries = checkResult.getOrThrow().entries
    assertThat(entries).containsExactly(entry("authenticated", false))
  }

  @Test
  fun `resetToFactory should fail with incorrect factory password`() {
    val result =
        AuthManager.resetToFactory(
            ResetToFactoryRequest("WrongFactoryPassword", serialNumber),
        )

    assertTrue(result.isError)
  }

  @Test
  fun `resetToFactory should fail with incorrect serial number`() {
    val result =
        AuthManager.resetToFactory(
            ResetToFactoryRequest(password, "WrongSerial"),
        )

    assertTrue(result.isError)
  }

  @Test
  fun `resetToFactory should fail when factory password file does not exist`() {
    factoryPasswordFile.delete()

    val result =
        AuthManager.resetToFactory(
            ResetToFactoryRequest(password, serialNumber),
        )

    assertTrue(result.isError)
  }

  @Test
  fun `resetToFactory should fail when serial number file does not exist`() {
    serialNumberFile.delete()

    val result =
        AuthManager.resetToFactory(
            ResetToFactoryRequest(password, serialNumber),
        )

    assertTrue(result.isError)
  }

  @Test
  fun `resetToFactory should fail when bcrypt fails`() {
    mockkObject(ConfigManager)
    every { ConfigManager.upsertConfig(any<Env>(), any<String>()) } throws
        RuntimeException("Simulated failure")

    val exception =
        assertThrows<RuntimeException> {
          AuthManager.resetToFactory(
              ResetToFactoryRequest(password, serialNumber),
          )
        }

    assertThat("Simulated failure").isEqualTo(exception.message)
  }

  @Test
  fun `changePassword should fail when ConfigManager fails`() {
    val loginResult = AuthManager.login(LoginRequest(password))
    assertTrue(loginResult.isSuccess)
    val token = loginResult.getOrThrow().token
    mockkObject(ConfigManager)
    every { ConfigManager.upsertConfig(any<Env>(), any<String>()) } throws
        RuntimeException("Simulated failure")

    val exception =
        assertThrows<RuntimeException> {
          AuthManager.changePassword(
              "Bearer $token",
              ChangePasswordRequest(password, "NewPassword123", serialNumber),
          )
        }

    assertThat("Simulated failure").isEqualTo(exception.message)
  }
}
