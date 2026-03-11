package com.jonquass.guardianhub.resource

import com.jonquass.guardianhub.GrizzlyServerExtension
import com.jonquass.guardianhub.core.api.TimezoneResponse
import com.jonquass.guardianhub.core.api.UpdateTimezoneResponse
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import jakarta.ws.rs.core.Response
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
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
    assertThat(response.timezones).isSortedAccordingTo(compareBy { it })
  }

  @Test
  fun `update timezone returns 401 without auth`() {
    Given {
      contentType(ContentType.JSON)
      body("""{"timezone": "America/New_York"}""")
    } When { post("/api/timezone") } Then { statusCode(Response.Status.UNAUTHORIZED.statusCode) }
  }

  @Test
  fun `update timezones returns 200 with valid timezone`() {
    val token = GrizzlyServerExtension.loginAndGetToken()

    val response =
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
              body("taskId", notNullValue())
              body(
                  "message",
                  equalTo("Timezone updated to America/New_York. Services are restarting."))
            } Extract
            {
              `as`(UpdateTimezoneResponse::class.java)
            }

    assertThat(response.taskId).isNotBlank()
    assertThat(response.message).contains("America/New_York")
  }

  @Test
  fun `update timezones returns 400 with invalid timezone`() {
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
          body("message", equalTo("Invalid timezone: Totally/Fake."))
        }
  }

  @Test
  fun `update timezones returns 400 with empty timezone`() {
    val token = GrizzlyServerExtension.loginAndGetToken()

    Given {
      header("Authorization", "Bearer $token")
      contentType(ContentType.JSON)
      body("""{"timezone": ""}""")
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
