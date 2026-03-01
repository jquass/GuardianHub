package com.jonquass.guardianhub.resource

import com.jonquass.guardianhub.GrizzlyServerExtension
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
  fun `login returns 200 with valid credentials`() {
    Given {
      contentType(ContentType.JSON)
      body("""{"password": "password123"}""")
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
    // Login first to get a token
    val token =
        Given {
          contentType(ContentType.JSON)
          body("""{"password": "password123"}""")
        } When
            {
              post("/api/auth/login")
            } Then
            {
              statusCode(200)
            } Extract
            {
              path<String>("token")
            }

    // Use token to check auth
    Given { header("Authorization", "Bearer $token") } When
        {
          get("/api/auth/check")
        } Then
        {
          statusCode(200)
        }
  }

  @Test
  fun `logout returns 200 with valid token`() {
    val token =
        Given {
          contentType(ContentType.JSON)
          body("""{"password": "password123"}""")
        } When
            {
              post("/api/auth/login")
            } Then
            {
              statusCode(200)
            } Extract
            {
              path<String>("token")
            }

    Given { header("Authorization", "Bearer $token") } When
        {
          post("/api/auth/logout")
        } Then
        {
          statusCode(200)
        }
  }

  @Test
  fun `change-password returns 200 with valid token`() {
    val token =
        Given {
          contentType(ContentType.JSON)
          body("""{"password": "password123"}""")
        } When
            {
              post("/api/auth/login")
            } Then
            {
              statusCode(200)
            } Extract
            {
              path<String>("token")
            }

    Given {
      contentType(ContentType.JSON)
      header("Authorization", "Bearer $token")
      body(
          """{"currentPassword": "password123", "serialNumber": "serial123", "newPassword": "123wordpass"}""")
    } When { post("/api/auth/change-password") } Then { statusCode(200) }

    Given {
      contentType(ContentType.JSON)
      body("""{"password": "123wordpass"}""")
    } When { post("/api/auth/login") } Then { statusCode(200) }
  }
}
