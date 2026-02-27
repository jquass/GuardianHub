package com.jonquass.guardianhub.manager

import com.jonquass.guardianhub.core.config.Env
import com.jonquass.guardianhub.core.manager.getOrThrow
import java.io.File
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConfigManagerTest {
  private lateinit var tempFile: File

  @BeforeEach
  fun setUp() {
    tempFile = File.createTempFile("test-env", ".env")
    ConfigManager.configFile = tempFile
  }

  @AfterEach
  fun tearDown() {
    tempFile.delete()
    ConfigManager.configFile = File(ConfigManager.DEFAULT_CONFIG_PATH)
  }

  @Test
  fun `readConfig should return entries for known env keys`() {
    tempFile.writeText(
        """
            GUARDIAN_IP=0.0.0.1
            SOMETHING_RANDOM=toot_toot
            ROUTER_IP=0.0.0.0
            ANOTHER_RANDOM=blah_blah_blah
            """
            .trimIndent(),
    )

    val result = ConfigManager.readConfig()

    Assertions.assertTrue(result.isSuccess)
    val entries = result.getOrThrow().entries
    org.assertj.core.api.Assertions.assertThat(entries).hasSize(2)
    org.assertj.core.api.Assertions.assertThat(entries)
        .extracting({ it.key }, { it.value })
        .containsExactly(
            org.assertj.core.api.Assertions.tuple("GUARDIAN_IP", "0.0.0.1"),
            org.assertj.core.api.Assertions.tuple("ROUTER_IP", "0.0.0.0"),
        )
  }

  @Test
  fun `readConfig should mask sensitive values`() {
    tempFile.writeText("LOGIN_PASSWORD=super_secret_password\n")

    val response = ConfigManager.readConfig()

    Assertions.assertTrue(response.isSuccess)
    val entries = response.getOrThrow().entries
    org.assertj.core.api.Assertions.assertThat(entries).hasSize(1)
    org.assertj.core.api.Assertions.assertThat(entries)
        .extracting({ it.key }, { it.value })
        .containsExactly(
            org.assertj.core.api.Assertions.tuple("LOGIN_PASSWORD", ConfigManager.SENSITIVE_MASK),
        )
  }

  @Test
  fun `readConfig should ignore comment lines`() {
    tempFile.writeText(
        """
            # This is a comment
            GUARDIAN_IP=0.0.0.1
            """
            .trimIndent(),
    )

    val result = ConfigManager.readConfig()

    org.assertj.core.api.Assertions.assertThat(result.isSuccess).isTrue()
    val response = result.getOrThrow()
    org.assertj.core.api.Assertions.assertThat(response.entries).hasSize(1)
  }

  @Test
  fun `readConfig should ignore empty lines`() {
    tempFile.writeText(
        """
            GUARDIAN_IP=0.0.0.1
            
            ROUTER_IP=0.0.0.0
            """
            .trimIndent(),
    )

    val result = ConfigManager.readConfig()

    org.assertj.core.api.Assertions.assertThat(result.isSuccess).isTrue()
    val response = result.getOrThrow()
    org.assertj.core.api.Assertions.assertThat(response.entries).hasSize(2)
  }

  @Test
  fun `readConfig should return correct categories`() {
    tempFile.writeText("GUARDIAN_IP=0.0.0.1\n")

    val result = ConfigManager.readConfig()

    org.assertj.core.api.Assertions.assertThat(result.isSuccess).isTrue()
    val response = result.getOrThrow()
    org.assertj.core.api.Assertions.assertThat(response.categories).hasSize(1)
    org.assertj.core.api.Assertions.assertThat(response.categories.first().name)
        .isEqualTo(Env.GUARDIAN_IP.category.displayName)
  }

  @Test
  fun `readConfig should return empty entries for empty file`() {
    tempFile.writeText("")

    val result = ConfigManager.readConfig()

    org.assertj.core.api.Assertions.assertThat(result.isSuccess).isTrue()
    val response = result.getOrThrow()
    org.assertj.core.api.Assertions.assertThat(response.entries).isEmpty()
    org.assertj.core.api.Assertions.assertThat(response.categories).isEmpty()
  }

  @Test
  fun `getRawConfigValue should return value for existing key`() {
    tempFile.writeText("GUARDIAN_IP=0.0.0.1\n")

    val result = ConfigManager.getRawConfigValue(Env.GUARDIAN_IP)

    Assertions.assertEquals("0.0.0.1", result)
  }

  @Test
  fun `getRawConfigValue should return sensitive values`() {
    tempFile.writeText("LOGIN_PASSWORD=secret_password\n")

    val result = ConfigManager.getRawConfigValue(Env.LOGIN_PASSWORD)

    Assertions.assertEquals("secret_password", result)
  }

  @Test
  fun `getRawConfigValue should strip surrounding single quotes`() {
    tempFile.writeText("GUARDIAN_IP='0.0.0.0'\n")

    val result = ConfigManager.getRawConfigValue(Env.GUARDIAN_IP)

    Assertions.assertEquals("0.0.0.0", result)
  }

  @Test
  fun `getRawConfigValue should return null for missing key`() {
    tempFile.writeText("GUARDIAN_IP=0.0.0.1\n")

    val result = ConfigManager.getRawConfigValue(Env.ROUTER_IP)

    org.assertj.core.api.Assertions.assertThat(result).isNull()
  }

  @Test
  fun `getRawConfigValue should return null for empty file`() {
    tempFile.writeText("")

    val result = ConfigManager.getRawConfigValue(Env.ROUTER_IP)

    org.assertj.core.api.Assertions.assertThat(result).isNull()
  }

  @Test
  fun `getRawConfigValue should strip surrounding double quotes`() {
    tempFile.writeText("GUARDIAN_IP=\"0.0.0.0\"\n")

    val result = ConfigManager.getRawConfigValue(Env.GUARDIAN_IP)

    Assertions.assertEquals("0.0.0.0", result)
  }

  @Test
  fun `getRawConfigValue should handle value containing equals sign`() {
    tempFile.writeText("LOGIN_PASSWORD=abc=def==\n")

    val result = ConfigManager.getRawConfigValue(Env.LOGIN_PASSWORD)

    Assertions.assertEquals("abc=def==", result)
  }

  @Test
  fun `upsertConfig should add new key if not present`() {
    tempFile.writeText("LOGIN_PASSWORD=secret_password\n")

    ConfigManager.upsertConfig(Env.GUARDIAN_IP, "0.0.0.1")

    Assertions.assertTrue(tempFile.readText().contains("GUARDIAN_IP=0.0.0.1"))
  }

  @Test
  fun `upsertConfig should add new key if empty env`() {
    tempFile.writeText("")

    ConfigManager.upsertConfig(Env.GUARDIAN_IP, "0.0.0.1")

    Assertions.assertTrue(tempFile.readText().contains("GUARDIAN_IP=0.0.0.1"))
  }

  @Test
  fun `upsertConfig should update existing key`() {
    tempFile.writeText("GUARDIAN_IP=0.0.0.1\n")

    ConfigManager.upsertConfig(Env.GUARDIAN_IP, "0.0.0.2")

    val contents = tempFile.readText()
    Assertions.assertTrue(contents.contains("GUARDIAN_IP=0.0.0.2"))
    Assertions.assertFalse(contents.contains("0.0.0.1"))
  }

  @Test
  fun `upsertConfig should wrap sensitive values in single quotes`() {
    tempFile.writeText("")

    ConfigManager.upsertConfig(Env.LOGIN_PASSWORD, "my_secret")

    Assertions.assertTrue(tempFile.readText().contains("LOGIN_PASSWORD='my_secret'"))
  }

  @Test
  fun `upsertConfig should not duplicate key`() {
    tempFile.writeText("GUARDIAN_IP=0.0.0.1\n")

    ConfigManager.upsertConfig(Env.GUARDIAN_IP, "0.0.0.2")

    val occurrences = tempFile.readLines().count { it.startsWith("GUARDIAN_IP=") }
    org.assertj.core.api.Assertions.assertThat(occurrences).isEqualTo(1)
  }

  @Test
  fun `upsertConfig should preserve other keys when updating`() {
    tempFile.writeText(
        """
            GUARDIAN_IP=0.0.0.1
            ROUTER_IP=0.0.0.0
            """
            .trimIndent(),
    )

    ConfigManager.upsertConfig(Env.GUARDIAN_IP, "0.0.0.2")

    Assertions.assertTrue(tempFile.readText().contains("ROUTER_IP=0.0.0.0"))
  }
}
