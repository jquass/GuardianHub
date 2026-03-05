package com.jonquass.guardianhub.resource

import com.jonquass.guardianhub.GrizzlyServerExtension
import com.jonquass.guardianhub.core.api.HomepageLinkResponse
import io.mockk.every
import io.mockk.mockk
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import jakarta.ws.rs.core.Response
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.InetAddress

@ExtendWith(GrizzlyServerExtension::class)
class HomepageResourceIT {

  @Test
  fun `homepage link returns 401 with invalid credentials`() {
    When { get("/api/homepage/link") } Then { statusCode(Response.Status.UNAUTHORIZED.statusCode) }
  }

  @Test
  fun `homepage link returns 200 with valid token`() {
    val token = GrizzlyServerExtension.loginAndGetToken()
    every { InetAddress.getByName(any()) } returns mockk()

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
}
