package com.jonquass.guardianhub.resource

import com.jonquass.guardianhub.GrizzlyServerExtension
import com.jonquass.guardianhub.GrizzlyServerExtension.Companion.TEST_PASSWORD
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import jakarta.ws.rs.core.Response
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(GrizzlyServerExtension::class)
class PasswordResourceIT {

  @Test
  fun `pihole password update returns 401 without auth`() {
    Given {
      contentType(ContentType.JSON)
      body("""{"password": "piholePassword"}""")
    } When
        {
          post("/api/password/pihole")
        } Then
        {
          statusCode(Response.Status.UNAUTHORIZED.statusCode)
        }
  }

  @Test
  fun `pihole password update returns 200 with valid token`() {
    val token = GrizzlyServerExtension.loginAndGetToken(TEST_PASSWORD)

    Given {
      contentType(ContentType.JSON)
      header("Authorization", "Bearer $token")
      body(
          """
              {"password": "piholePassword"}
              """
              .trimIndent())
    } When { post("/api/password/pihole") } Then { statusCode(Response.Status.OK.statusCode) }
  }

  @Test
  fun `wireguard password update returns 401 without auth`() {
    Given {
      contentType(ContentType.JSON)
      body("""{"password": "wireguardPassword"}""")
    } When
        {
          post("/api/password/wireguard")
        } Then
        {
          statusCode(Response.Status.UNAUTHORIZED.statusCode)
        }
  }

  @Test
  fun `wireguard password update returns 200 with valid token`() {
    val token = GrizzlyServerExtension.loginAndGetToken(TEST_PASSWORD)

    Given {
      contentType(ContentType.JSON)
      header("Authorization", "Bearer $token")
      body(
          """
              {"password": "wireguardPassword"}
              """
              .trimIndent())
    } When { post("/api/password/wireguard") } Then { statusCode(Response.Status.OK.statusCode) }
  }

  @Test
  fun `npm password update returns 401 without auth`() {
    Given {
      contentType(ContentType.JSON)
      body("""{"password": "npmPassword"}""")
    } When
        {
          post("/api/password/npm")
        } Then
        {
          statusCode(Response.Status.UNAUTHORIZED.statusCode)
        }
  }

  @Test
  fun `npm password update returns 200 with valid token`() {
    val token = GrizzlyServerExtension.loginAndGetToken(TEST_PASSWORD)

    Given {
      contentType(ContentType.JSON)
      header("Authorization", "Bearer $token")
      body(
          """
              {"password": "npmPassword"}
              """
              .trimIndent())
    } When { post("/api/password/npm") } Then { statusCode(Response.Status.OK.statusCode) }
  }
}
