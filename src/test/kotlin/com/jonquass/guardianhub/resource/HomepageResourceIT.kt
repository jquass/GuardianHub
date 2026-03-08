package com.jonquass.guardianhub.resource

import com.jonquass.guardianhub.GrizzlyServerExtension
import com.jonquass.guardianhub.core.api.HomepageLinkResponse
import com.jonquass.guardianhub.manager.HomepageManager
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import jakarta.ws.rs.core.Response
import java.net.InetAddress
import java.net.UnknownHostException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(GrizzlyServerExtension::class)
class HomepageResourceIT {

  @AfterEach
  fun tearDown() {
    HomepageManager.dnsResolver = { hostname ->
      try {
        InetAddress.getByName(hostname)
        true
      } catch (e: UnknownHostException) {
        false
      }
    }
  }

  @Test
  fun `homepage link returns 401 with invalid credentials`() {
    When { get("/api/homepage/link") } Then { statusCode(Response.Status.UNAUTHORIZED.statusCode) }
  }

  @Test
  fun `homepage link returns 200 with dns success`() {
    HomepageManager.dnsResolver = { true }
    val token = GrizzlyServerExtension.loginAndGetToken()

    val response =
        Given { header("Authorization", "Bearer $token") } When
            {
              get("/api/homepage/link")
            } Then
            {
              statusCode(Response.Status.OK.statusCode)
            } Extract
            {
              `as`(HomepageLinkResponse::class.java)
            }

    assertThat(response.usedDns).isEqualTo(true)
    assertThat(response.url).isEqualTo("http://homepage.guardian.home")
  }

  @Test
  fun `homepage link returns 200 with dns failure falls back to configured ip`() {
    HomepageManager.dnsResolver = { false }
    val token = GrizzlyServerExtension.loginAndGetToken()

    val response =
        Given { header("Authorization", "Bearer $token") } When
            {
              get("/api/homepage/link")
            } Then
            {
              statusCode(Response.Status.OK.statusCode)
            } Extract
            {
              `as`(HomepageLinkResponse::class.java)
            }

    assertThat(response.usedDns).isEqualTo(false)
    assertThat(response.url).startsWith("http://")
  }
}
