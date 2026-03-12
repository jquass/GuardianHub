package com.jonquass.guardianhub.manager.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import com.jonquass.guardianhub.core.getOrThrow
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PasswordHashManagerTest {
  private val password = "MySecurePassword"

  @AfterEach
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `hashPassword should create valid bcrypt hash`() {
    val hash = PasswordHashManager.hashPasswordResult(password).getOrThrow()

    assertTrue(hash.startsWith("\$2a\$") || hash.startsWith("\$2b\$"))
    assertTrue(hash.length == 60)
  }

  @Test
  fun `hashPassword should create different hashes for same password`() {
    val hash1 = PasswordHashManager.hashPasswordResult(password).getOrThrow()
    val hash2 = PasswordHashManager.hashPasswordResult(password).getOrThrow()

    assertTrue(hash1 != hash2)
    assertTrue(hash1.startsWith("\$2a\$") || hash1.startsWith("\$2b\$"))
    assertTrue(hash2.startsWith("\$2a\$") || hash2.startsWith("\$2b\$"))
  }

  @Test
  fun `verifyHash should work on hashed password`() {
    val hash = PasswordHashManager.hashPasswordResult(password).getOrThrow()

    val result = PasswordHashManager.verifyHash(password, hash)

    assertThat(result.isSuccess).isTrue()
  }

  @Test
  fun `verifyHash should not work on different passwords`() {
    val hash = PasswordHashManager.hashPasswordResult(password).getOrThrow()

    val result = PasswordHashManager.verifyHash("AnotherPassword", hash)

    assertThat(result.isError).isTrue()
  }

  @Test
  fun `verifyHash should return false and swallow exception when BCrypt verifyer throws`() {
    mockkStatic(BCrypt::class)
    every { BCrypt.verifyer() } throws RuntimeException("Simulated BCrypt failure")

    val result = PasswordHashManager.verifyHash("anyPassword", "\$2a\$10\$invalidhash")

    assertThat(result.isError).isTrue()
  }

  @Test
  fun `verifyHash should return false when hash is malformed and causes exception`() {
    val result = PasswordHashManager.verifyHash("anyPassword", "not-a-valid-bcrypt-hash")

    assertThat(result.isError).isTrue()
  }

  @Test
  fun `hashPassword should rethrow the original exception BCrypt throws`() {
    mockkStatic(BCrypt::class)
    every { BCrypt.withDefaults() } throws IllegalStateException("Illegal state")

    val result = PasswordHashManager.hashPasswordResult("anyPassword")

    assertThat(result.isError).isTrue()
  }
}
