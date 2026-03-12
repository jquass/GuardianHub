package com.jonquass.guardianhub.resource

import com.jonquass.guardianhub.GrizzlyServerExtension
import com.jonquass.guardianhub.core.api.UpdatePasswordRequest
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
      body(UpdatePasswordRequest("piholePassword"))
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
    val token = GrizzlyServerExtension.loginAndGetToken()

    Given {
      contentType(ContentType.JSON)
      header("Authorization", "Bearer $token")
      body(UpdatePasswordRequest("piholePassword"))
    } When { post("/api/password/pihole") } Then { statusCode(Response.Status.OK.statusCode) }
  }

  @Test
  fun `wireguard password update returns 401 without auth`() {
    Given {
      contentType(ContentType.JSON)
      body(UpdatePasswordRequest("wireguardPassword"))
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
    val token = GrizzlyServerExtension.loginAndGetToken()

    Given {
      contentType(ContentType.JSON)
      header("Authorization", "Bearer $token")
      body(UpdatePasswordRequest("wireguardPassword"))
    } When { post("/api/password/wireguard") } Then { statusCode(Response.Status.OK.statusCode) }
  }

  @Test
  fun `npm password update returns 401 without auth`() {
    Given {
      contentType(ContentType.JSON)
      body(UpdatePasswordRequest("npmPassword"))
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
    val token = GrizzlyServerExtension.loginAndGetToken()

    Given {
      contentType(ContentType.JSON)
      header("Authorization", "Bearer $token")
      body(UpdatePasswordRequest("npmPassword"))
    } When { post("/api/password/npm") } Then { statusCode(Response.Status.OK.statusCode) }
  }
}
