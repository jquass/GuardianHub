package com.jonquass.guardianhub.manager

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import jakarta.ws.rs.core.Response
import java.io.File
import java.net.InetAddress
import java.net.UnknownHostException
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
    mockkStatic(InetAddress::class)
  }

  @AfterEach
  fun tearDown() {
    tempFile.delete()
    ConfigManager.configFile = File(ConfigManager.DEFAULT_CONFIG_PATH)
    unmockkStatic(InetAddress::class)
  }

  @Test
  fun `getHomepageLink returns 200`() {
    every { InetAddress.getByName("homepage.guardian.home") } returns mockk()

    val response = HomepageManager.getHomepageLink()

    assertThat(response.status).isEqualTo(Response.Status.OK.statusCode)
  }

  @Test
  fun `getHomepageLink returns success status in body`() {
    every { InetAddress.getByName("homepage.guardian.home") } returns mockk()

    val response = HomepageManager.getHomepageLink()
    val body = response.entity as Map<*, *>

    assertThat(body["status"]).isEqualTo("success")
  }

  @Test
  fun `getHomepageLink returns url in body`() {
    every { InetAddress.getByName("homepage.guardian.home") } returns mockk()

    val response = HomepageManager.getHomepageLink()
    val body = response.entity as Map<*, *>

    assertThat(body["url"]).isNotNull()
    assertThat(body["url"] as String).startsWith("http://")
  }

  @Test
  fun `getHomepageLink returns usedDns boolean in body`() {
    every { InetAddress.getByName("homepage.guardian.home") } returns mockk()

    val response = HomepageManager.getHomepageLink()
    val body = response.entity as Map<*, *>

    assertThat(body["usedDns"]).isIn(true, false)
  }

  @Test
  fun `getHomepageLink returns true for usedDns when DNS resolution succeeds`() {
    every { InetAddress.getByName("homepage.guardian.home") } returns mockk()

    val response = HomepageManager.getHomepageLink()
    val body = response.entity as Map<*, *>

    assertThat(body["usedDns"]).isEqualTo(true)
    assertThat(body["url"]).isEqualTo("http://homepage.guardian.home")
  }

  @Test
  fun `getHomepageLink returns false for usedDns when DNS resolution fails`() {
    every { InetAddress.getByName("homepage.guardian.home") } throws
        UnknownHostException("DNS failed")

    val response = HomepageManager.getHomepageLink()
    val body = response.entity as Map<*, *>

    assertThat(body["usedDns"]).isEqualTo(false)
  }

  @Test
  fun `getHomepageLink falls back to configured IP when DNS fails`() {
    tempFile.writeText("GUARDIAN_IP=1.2.3.4\n")
    every { InetAddress.getByName("homepage.guardian.home") } throws
        UnknownHostException("DNS failed")

    val response = HomepageManager.getHomepageLink()
    val body = response.entity as Map<*, *>

    assertThat(body["url"]).isEqualTo("http://1.2.3.4:3001")
  }

  @Test
  fun `getHomepageLink falls back to 127_0_0_1 when DNS fails and no IP configured`() {
    tempFile.writeText("")
    every { InetAddress.getByName("homepage.guardian.home") } throws
        UnknownHostException("DNS failed")

    val response = HomepageManager.getHomepageLink()
    val body = response.entity as Map<*, *>

    assertThat(body["url"]).isEqualTo("http://127.0.0.1:3001")
  }
}
