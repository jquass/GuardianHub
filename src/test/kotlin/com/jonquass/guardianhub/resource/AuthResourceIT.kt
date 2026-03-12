package com.jonquass.guardianhub.resource

import com.jonquass.guardianhub.GrizzlyServerExtension
import com.jonquass.guardianhub.GrizzlyServerExtension.Companion.TEST_PASSWORD
import com.jonquass.guardianhub.GrizzlyServerExtension.Companion.TEST_SERIAL_NUMBER
import com.jonquass.guardianhub.core.api.auth.ChangePasswordRequest
import com.jonquass.guardianhub.core.api.auth.CheckAuthResponse
import com.jonquass.guardianhub.core.api.auth.LoginRequest
import com.jonquass.guardianhub.core.api.auth.LoginResponse
import com.jonquass.guardianhub.core.api.auth.ResetToFactoryRequest
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import jakarta.ws.rs.core.Response
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(GrizzlyServerExtension::class)
class AuthResourceIT {

  @Test
  fun `login returns 200 with valid credentials and does not require auth header`() {
    val response =
        Given {
          contentType(ContentType.JSON)
          body(LoginRequest(TEST_PASSWORD))
        } When
            {
              post("/api/auth/login")
            } Then
            {
              statusCode(Response.Status.OK.statusCode)
            } Extract
            {
              `as`(LoginResponse::class.java)
            }

    assertThat(response.success).isTrue
    assertThat(response.token).isNotNull
  }

  @Test
  fun `login returns 401 with invalid credentials`() {
    Given {
      contentType(ContentType.JSON)
      body(LoginRequest("wrong-password"))
    } When { post("/api/auth/login") } Then { statusCode(Response.Status.UNAUTHORIZED.statusCode) }
  }

  @Test
  fun `check returns 200 with authenticated false without auth`() {
    val response =
        When { get("/api/auth/check") } Then
            {
              statusCode(Response.Status.OK.statusCode)
            } Extract
            {
              `as`(CheckAuthResponse::class.java)
            }

    assertThat(response.authenticated).isFalse
  }

  @Test
  fun `check returns 200 with authenticated true with valid token`() {
    val token = GrizzlyServerExtension.loginAndGetToken()

    val response =
        Given { header("Authorization", "Bearer $token") } When
            {
              get("/api/auth/check")
            } Then
            {
              statusCode(Response.Status.OK.statusCode)
            } Extract
            {
              `as`(CheckAuthResponse::class.java)
            }

    assertThat(response.authenticated).isTrue
  }

  @Test
  fun `logout returns 200 with valid token`() {
    val token = GrizzlyServerExtension.loginAndGetToken()

    Given { header("Authorization", "Bearer $token") } When
        {
          post("/api/auth/logout")
        } Then
        {
          statusCode(Response.Status.OK.statusCode)
        }
  }

  @Test
  fun `change-password returns 200 and new password is valid for login`() {
    val newPassword = "123wordpass"
    val token = GrizzlyServerExtension.loginAndGetToken()

    Given {
      contentType(ContentType.JSON)
      header("Authorization", "Bearer $token")
      body(ChangePasswordRequest(TEST_PASSWORD, newPassword, TEST_SERIAL_NUMBER))
    } When { post("/api/auth/change-password") } Then { statusCode(Response.Status.OK.statusCode) }

    val newToken = GrizzlyServerExtension.loginAndGetToken(newPassword)
    val checkResponse =
        Given { header("Authorization", "Bearer $newToken") } When
            {
              get("/api/auth/check")
            } Then
            {
              statusCode(Response.Status.OK.statusCode)
            } Extract
            {
              `as`(CheckAuthResponse::class.java)
            }

    assertThat(checkResponse.authenticated).isTrue
  }

  @Test
  fun `reset-to-factory resets password to factory default without auth header`() {
    val newPassword = "123wordpass"
    val token = GrizzlyServerExtension.loginAndGetToken()

    Given {
      contentType(ContentType.JSON)
      header("Authorization", "Bearer $token")
      body(ChangePasswordRequest(TEST_PASSWORD, newPassword, TEST_SERIAL_NUMBER))
    } When { post("/api/auth/change-password") } Then { statusCode(Response.Status.OK.statusCode) }

    Given {
      contentType(ContentType.JSON)
      body(ResetToFactoryRequest(TEST_PASSWORD, TEST_SERIAL_NUMBER))
    } When { post("/api/auth/reset-to-factory") } Then { statusCode(Response.Status.OK.statusCode) }

    Given {
      contentType(ContentType.JSON)
      body(LoginRequest(newPassword))
    } When { post("/api/auth/login") } Then { statusCode(Response.Status.UNAUTHORIZED.statusCode) }

    Given {
      contentType(ContentType.JSON)
      body(LoginRequest(TEST_PASSWORD))
    } When { post("/api/auth/login") } Then { statusCode(Response.Status.OK.statusCode) }
  }
}
