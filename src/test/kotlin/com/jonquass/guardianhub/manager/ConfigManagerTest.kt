package com.jonquass.guardianhub.manager

import com.jonquass.guardianhub.core.config.Env
import com.jonquass.guardianhub.core.config.EnvCategory
import com.jonquass.guardianhub.core.exception.ConfigException
import com.jonquass.guardianhub.core.getOrThrow
import java.io.File
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows

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

    assertThat(result.isSuccess).isTrue()
    val entries = result.getOrThrow().entries
    assertThat(entries)
        .extracting({ it.key }, { it.value })
        .containsExactly(
            tuple("GUARDIAN_IP", "0.0.0.1"),
            tuple("ROUTER_IP", "0.0.0.0"),
        )
  }

  @Test
  fun `readConfig should mask sensitive values`() {
    tempFile.writeText("LOGIN_PASSWORD=super_secret_password\n")

    val result = ConfigManager.readConfig()

    assertThat(result.isSuccess).isTrue()
    val entries = result.getOrThrow().entries
    assertThat(entries)
        .extracting({ it.key }, { it.value })
        .containsExactly(
            tuple("LOGIN_PASSWORD", ConfigManager.SENSITIVE_MASK),
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

    assertThat(result.isSuccess).isTrue()
    val response = result.getOrThrow()
    assertThat(response.entries).hasSize(1)
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

    assertThat(result.isSuccess).isTrue()
    val response = result.getOrThrow()
    assertThat(response.entries).hasSize(2)
  }

  @Test
  fun `readConfig should return correct categories`() {
    tempFile.writeText("GUARDIAN_IP=0.0.0.1\n")

    val result = ConfigManager.readConfig()

    assertThat(result.isSuccess).isTrue()
    val response = result.getOrThrow()
    assertThat(response.categories).hasSize(1)
    assertThat(response.categories.first().name).isEqualTo(Env.GUARDIAN_IP.category.displayName)
  }

  @Test
  fun `readConfig should return empty entries for empty file`() {
    tempFile.writeText("")

    val result = ConfigManager.readConfig()

    assertThat(result.isSuccess).isTrue()
    val response = result.getOrThrow()
    assertThat(response.entries).isEmpty()
    assertThat(response.categories).isEmpty()
  }

  @Test
  fun `readConfig should ignore keys without values`() {
    tempFile.writeText("GUARDIAN_IP")

    val result = ConfigManager.readConfig()

    assertThat(result.isSuccess).isTrue()
    val response = result.getOrThrow()
    assertThat(response.entries).isEmpty()
    assertThat(response.categories).isEmpty()
  }

  @Test
  fun `readConfig should throw ConfigException if file is unreadable`() {
    tempFile.setReadable(false)

    assertThrows<ConfigException> { ConfigManager.readConfig() }

    tempFile.setReadable(true)
  }

  @Test
  fun `readConfig should only include categories that have matching entries`() {
    // Write two entries from potentially different categories
    tempFile.writeText(
        """
        GUARDIAN_IP=0.0.0.1
        ROUTER_IP=0.0.0.0
        """
            .trimIndent(),
    )

    val result = ConfigManager.readConfig()

    assertThat(result.isSuccess).isTrue()
    val response = result.getOrThrow()
    // All returned category names must correspond to actual entries
    val entryCategoryNames = response.entries.map { it.categoryName }.toSet()
    assertThat(response.categories.map { it.name }.toSet()).isEqualTo(entryCategoryNames)
  }

  @Test
  fun `readConfig categories should only contain entries matching a known EnvCategory`() {
    // Every Env entry must have a category whose displayName matches an EnvCategory,
    // so mapNotNull's null branch in readConfig is structurally unreachable.
    // This test guards that invariant at the enum level.
    Env.entries
        .filter { it != Env.UNKNOWN }
        .forEach { env ->
          val match = EnvCategory.entries.find { it.displayName == env.category.displayName }
          assertThat(match)
              .withFailMessage(
                  "Env.${env.name} has category '${env.category.displayName}' with no matching EnvCategory")
              .isNotNull()
        }
  }

  @Test
  fun `getRawConfigValue should return value for existing key`() {
    tempFile.writeText("GUARDIAN_IP=0.0.0.1\n")

    val result = ConfigManager.getRawConfigValue(Env.GUARDIAN_IP)

    assertThat(result.getOrThrow()).isEqualTo("0.0.0.1")
  }

  @Test
  fun `getRawConfigValue should return sensitive values`() {
    tempFile.writeText("LOGIN_PASSWORD=secret_password\n")

    val result = ConfigManager.getRawConfigValue(Env.LOGIN_PASSWORD)

    assertThat(result.getOrThrow()).isEqualTo("secret_password")
  }

  @Test
  fun `getRawConfigValue should strip surrounding single quotes`() {
    tempFile.writeText("GUARDIAN_IP='0.0.0.0'\n")

    val result = ConfigManager.getRawConfigValue(Env.GUARDIAN_IP)

    assertThat(result.isSuccess).isTrue()
    assertThat(result.getOrThrow()).isEqualTo("0.0.0.0")
  }

  @Test
  fun `getRawConfigValue should return error for missing key`() {
    tempFile.writeText("GUARDIAN_IP=0.0.0.1\n")

    val result = ConfigManager.getRawConfigValue(Env.ROUTER_IP)

    assertThat(result.isError).isTrue()
  }

  @Test
  fun `getRawConfigValue should return error for empty file`() {
    tempFile.writeText("")

    val result = ConfigManager.getRawConfigValue(Env.ROUTER_IP)

    assertThat(result.isError).isTrue()
  }

  @Test
  fun `getRawConfigValue should strip surrounding double quotes`() {
    tempFile.writeText("GUARDIAN_IP=\"0.0.0.0\"\n")

    val result = ConfigManager.getRawConfigValue(Env.GUARDIAN_IP)

    assertThat(result.isSuccess).isTrue()
    assertThat(result.getOrThrow()).isEqualTo("0.0.0.0")
  }

  @Test
  fun `getRawConfigValue should handle value containing equals sign`() {
    tempFile.writeText("LOGIN_PASSWORD=abc=def==\n")

    val result = ConfigManager.getRawConfigValue(Env.LOGIN_PASSWORD)

    assertThat(result.isSuccess).isTrue()
    assertThat(result.getOrThrow()).isEqualTo("abc=def==")
  }

  @Test
  fun `getRawConfigValue should skip lines without equals sign`() {
    tempFile.writeText(
        """
        GUARDIAN_IP
        ROUTER_IP=0.0.0.0
        """
            .trimIndent(),
    )

    val result = ConfigManager.getRawConfigValue(Env.ROUTER_IP)

    assertThat(result.isSuccess).isTrue()
    assertThat(result.getOrThrow()).isEqualTo("0.0.0.0")
  }

  @Test
  fun `getRawConfigValue should ignore comment lines`() {
    tempFile.writeText(
        """
              # This is a comment
              GUARDIAN_IP=0.0.0.1
              """
            .trimIndent(),
    )

    val result = ConfigManager.getRawConfigValue(Env.GUARDIAN_IP)

    assertThat(result.isSuccess).isTrue()
    assertThat(result.getOrThrow()).isEqualTo("0.0.0.1")
  }

  @Test
  fun `getRawConfigValue should return error if file is unreadable`() {
    tempFile.setReadable(false)

    assertThrows<ConfigException> { ConfigManager.getRawConfigValue(Env.GUARDIAN_IP) }

    tempFile.setReadable(true)
  }

  @Test
  fun `getRawConfigValue should skip empty lines and still find key`() {
    tempFile.writeText(
        """
        
        GUARDIAN_IP=0.0.0.1
        
        """
            .trimIndent(),
    )

    val result = ConfigManager.getRawConfigValue(Env.GUARDIAN_IP)

    assertThat(result.isSuccess).isTrue()
    assertThat(result.getOrThrow()).isEqualTo("0.0.0.1")
  }

  @Test
  fun `getRawConfigValue should return error when file has keys but none match`() {
    tempFile.writeText(
        """
        ROUTER_IP=0.0.0.0
        LOGIN_PASSWORD=secret
        """
            .trimIndent(),
    )

    val result = ConfigManager.getRawConfigValue(Env.GUARDIAN_IP)

    assertThat(result.isError).isTrue()
  }

  @Test
  fun `getRawConfigValue should not strip mismatched double quotes`() {
    tempFile.writeText("GUARDIAN_IP=\"0.0.0.1\n")

    val result = ConfigManager.getRawConfigValue(Env.GUARDIAN_IP)

    assertThat(result.isSuccess).isTrue()
    // Value should be returned as-is since closing quote is missing
    assertThat(result.getOrThrow()).isEqualTo("\"0.0.0.1")
  }

  @Test
  fun `getRawConfigValue should not strip mismatched single quotes`() {
    tempFile.writeText("GUARDIAN_IP='0.0.0.1\n")

    val result = ConfigManager.getRawConfigValue(Env.GUARDIAN_IP)

    assertThat(result.isSuccess).isTrue()
    // Value should be returned as-is since quotes don't match on both sides
    assertThat(result.getOrThrow()).isEqualTo("'0.0.0.1")
  }

  @Test
  fun `getRawConfigValue should return correct value when target key is not the first entry`() {
    tempFile.writeText(
        """
        ROUTER_IP=0.0.0.0
        GUARDIAN_IP=0.0.0.1
        """
            .trimIndent(),
    )

    val result = ConfigManager.getRawConfigValue(Env.GUARDIAN_IP)

    assertThat(result.isSuccess).isTrue()
    assertThat(result.getOrThrow()).isEqualTo("0.0.0.1")
  }

  @Test
  fun `upsertConfig should throw ConfigException if file is unreadable`() {
    tempFile.setReadable(false)

    assertThrows<ConfigException> { ConfigManager.upsertConfig(Env.GUARDIAN_IP, "0.0.0.1") }

    tempFile.setReadable(true)
  }

  @Test
  fun `upsertConfig should throw ConfigException if file is not writeable`() {
    tempFile.setWritable(false)

    assertThrows<ConfigException> { ConfigManager.upsertConfig(Env.GUARDIAN_IP, "0.0.0.1") }

    tempFile.setWritable(true)
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
    assertThat(contents).contains("GUARDIAN_IP=0.0.0.2")
    assertThat(contents).doesNotContain("0.0.0.1")
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
    assertThat(occurrences).isEqualTo(1)
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

  @Test
  fun `upsertConfig should wrap sensitive values in single quotes when updating`() {
    tempFile.writeText("LOGIN_PASSWORD='old_secret'\n")

    ConfigManager.upsertConfig(Env.LOGIN_PASSWORD, "new_secret")

    val contents = tempFile.readText()
    assertThat(contents).contains("LOGIN_PASSWORD='new_secret'")
    assertThat(contents).doesNotContain("old_secret")
  }
}
