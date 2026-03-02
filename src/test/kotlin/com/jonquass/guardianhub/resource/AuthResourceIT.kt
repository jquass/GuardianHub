package com.jonquass.guardianhub.resource

import com.jonquass.guardianhub.GrizzlyServerExtension
import com.jonquass.guardianhub.GrizzlyServerExtension.Companion.TEST_PASSWORD
import com.jonquass.guardianhub.GrizzlyServerExtension.Companion.TEST_SERIAL_NUMBER
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import jakarta.ws.rs.core.Response
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(GrizzlyServerExtension::class)
class AuthResourceIT {

  @Test
  fun `login returns 200 with valid credentials and does not require auth header`() {
    Given {
      contentType(ContentType.JSON)
      body("""{"password": "$TEST_PASSWORD"}""")
    } When
        {
          post("/api/auth/login")
        } Then
        {
          statusCode(Response.Status.OK.statusCode)
          body("success", equalTo(true))
          body("token", notNullValue())
        }
  }

  @Test
  fun `login returns 401 with invalid credentials`() {
    Given {
      contentType(ContentType.JSON)
      body("""{"password": "wrong"}""")
    } When
        {
          post("/api/auth/login")
        } Then
        {
          statusCode(Response.Status.UNAUTHORIZED.statusCode)
          body("token", nullValue())
        }
  }

  @Test
  fun `check returns 401 without auth`() {
    When { get("/api/auth/check") } Then { statusCode(Response.Status.UNAUTHORIZED.statusCode) }
  }

  @Test
  fun `check returns 200 with valid token`() {
    val token = GrizzlyServerExtension.loginAndGetToken(TEST_PASSWORD)

    Given { header("Authorization", "Bearer $token") } When
        {
          get("/api/auth/check")
        } Then
        {
          statusCode(Response.Status.OK.statusCode)
        }
  }

  @Test
  fun `logout returns 200 with valid token`() {
    val token = GrizzlyServerExtension.loginAndGetToken(TEST_PASSWORD)

    Given { header("Authorization", "Bearer $token") } When
        {
          post("/api/auth/logout")
        } Then
        {
          statusCode(Response.Status.OK.statusCode)
        }
  }

  @Test
  fun `change-password returns 200 with valid token`() {
    val newPassword = "123wordpass"
    val token = GrizzlyServerExtension.loginAndGetToken(TEST_PASSWORD)

    Given {
      contentType(ContentType.JSON)
      header("Authorization", "Bearer $token")
      body(
          """
              {"currentPassword": "$TEST_PASSWORD", 
              "serialNumber": "$TEST_SERIAL_NUMBER", 
              "newPassword": "$newPassword"}
              """
              .trimIndent())
    } When { post("/api/auth/change-password") } Then { statusCode(Response.Status.OK.statusCode) }

    GrizzlyServerExtension.loginAndGetToken(newPassword)
  }

  @Test
  fun `reset-to-factory does not require auth header`() {
    val newPassword = "123wordpass"
    val token =
        Given {
          contentType(ContentType.JSON)
          body("""{"password": "$TEST_PASSWORD"}""")
        } When
            {
              post("/api/auth/login")
            } Then
            {
              statusCode(Response.Status.OK.statusCode)
            } Extract
            {
              path<String>("token")
            }

    Given {
      contentType(ContentType.JSON)
      header("Authorization", "Bearer $token")
      body(
          """
              {"currentPassword": "$TEST_PASSWORD", 
              "serialNumber": "$TEST_SERIAL_NUMBER", 
              "newPassword": "$newPassword"}
              """
              .trimIndent())
    } When { post("/api/auth/change-password") } Then { statusCode(Response.Status.OK.statusCode) }

    Given {
      contentType(ContentType.JSON)
      body("""{"password": "$newPassword"}""")
    } When { post("/api/auth/login") } Then { statusCode(Response.Status.OK.statusCode) }

    Given {
      contentType(ContentType.JSON)
      body(
          """
              {"factoryPassword": "$TEST_PASSWORD", 
              "serialNumber": "$TEST_SERIAL_NUMBER"}
              """
              .trimIndent())
    } When { post("/api/auth/reset-to-factory") } Then { statusCode(Response.Status.OK.statusCode) }

    Given {
      contentType(ContentType.JSON)
      body("""{"password": "$newPassword"}""")
    } When { post("/api/auth/login") } Then { statusCode(Response.Status.UNAUTHORIZED.statusCode) }

    Given {
      contentType(ContentType.JSON)
      body("""{"password": "$TEST_PASSWORD"}""")
    } When { post("/api/auth/login") } Then { statusCode(Response.Status.OK.statusCode) }
  }
}
