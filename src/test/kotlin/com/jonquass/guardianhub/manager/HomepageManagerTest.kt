package com.jonquass.guardianhub.manager

import jakarta.ws.rs.core.Response
import java.io.File
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HomepageManagerTest {
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
  fun `getHomepageLink returns 200`() {
    val response = HomepageManager.getHomepageLink()

    assertThat(response.status).isEqualTo(Response.Status.OK.statusCode)
  }

  @Test
  fun `getHomepageLink returns success status in body`() {
    val response = HomepageManager.getHomepageLink()
    val body = response.entity as Map<*, *>

    assertThat(body["status"]).isEqualTo("success")
  }

  @Test
  fun `getHomepageLink returns url in body`() {
    val response = HomepageManager.getHomepageLink()
    val body = response.entity as Map<*, *>

    assertThat(body["url"]).isNotNull()
    assertThat(body["url"] as String).startsWith("http://")
  }

  @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
  @Test
  fun `getHomepageLink returns usedDns boolean in body`() {
    val response = HomepageManager.getHomepageLink()
    val body = response.entity as Map<*, *>

    assertThat(body["usedDns"]).isInstanceOf(java.lang.Boolean::class.java)
  }

  @Test
  fun `getHomepageLink falls back to configured IP when DNS fails`() {
    tempFile.writeText("GUARDIAN_IP=1.2.3.4\n")

    val response = HomepageManager.getHomepageLink()
    val body = response.entity as Map<*, *>

    // DNS for homepage.guardian.home will fail in test environment
    val usedDns = body["usedDns"] as Boolean
    if (!usedDns) {
      assertThat(body["url"]).isEqualTo("http://1.2.3.4:3001")
    }
  }

  @Test
  fun `getHomepageLink falls back to 127_0_0_1 when DNS fails and no IP configured`() {
    tempFile.writeText("")

    val response = HomepageManager.getHomepageLink()
    val body = response.entity as Map<*, *>

    val usedDns = body["usedDns"] as Boolean
    if (!usedDns) {
      assertThat(body["url"]).isEqualTo("http://127.0.0.1:3001")
    }
  }

  @Test
  fun `getHomepageLink uses DNS url when DNS resolves`() {
    val response = HomepageManager.getHomepageLink()
    val body = response.entity as Map<*, *>

    val usedDns = body["usedDns"] as Boolean
    if (usedDns) {
      assertThat(body["url"]).isEqualTo("http://homepage.guardian.home")
    }
  }
}
