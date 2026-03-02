package com.jonquass.guardianhub.resource

import com.jonquass.guardianhub.GrizzlyServerExtension
import com.jonquass.guardianhub.core.api.TimezoneResponse
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import jakarta.ws.rs.core.Response
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(GrizzlyServerExtension::class)
class TimezoneResourceIT {

  @Test
  fun `get timezones returns 401 without auth`() {
    When { get("/api/timezone") } Then { statusCode(Response.Status.UNAUTHORIZED.statusCode) }
  }

  @Test
  fun `get timezones returns 200 with auth`() {
    val token = GrizzlyServerExtension.loginAndGetToken()

    val response =
        Given { header("Authorization", "Bearer $token") } When
            {
              get("/api/timezone")
            } Then
            {
              statusCode(Response.Status.OK.statusCode)
            } Extract
            {
              `as`(TimezoneResponse::class.java)
            }

    assertThat(response.timezones).contains("UTC", "America/New_York")
  }

  @Test
  fun `update timezone returns 401 without auth`() {
    Given {
      contentType(ContentType.JSON)
      body("""{"timezone": "America/New_York"}""")
    } When { post("/api/timezone") } Then { statusCode(Response.Status.UNAUTHORIZED.statusCode) }
  }

  @Test
  fun `update timezones returns 200 with auth`() {
    val token = GrizzlyServerExtension.loginAndGetToken()

    Given {
      header("Authorization", "Bearer $token")
      contentType(ContentType.JSON)
      body("""{"timezone": "America/New_York"}""")
    } When
        {
          post("/api/timezone")
        } Then
        {
          statusCode(Response.Status.OK.statusCode)
          body("status", equalTo("success"))
        }
  }

  @Test
  fun `update timezones returns 400 with bad input`() {
    val token = GrizzlyServerExtension.loginAndGetToken()

    Given {
      header("Authorization", "Bearer $token")
      contentType(ContentType.JSON)
      body("""{"timezone": "Totally/Fake"}""")
    } When
        {
          post("/api/timezone")
        } Then
        {
          statusCode(Response.Status.BAD_REQUEST.statusCode)
          body("status", equalTo("error"))
        }
  }
}
