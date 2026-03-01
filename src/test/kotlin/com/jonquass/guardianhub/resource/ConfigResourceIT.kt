package com.jonquass.guardianhub.resource

import com.jonquass.guardianhub.GrizzlyServerExtension
import com.jonquass.guardianhub.GrizzlyServerExtension.Companion.TEST_PASSWORD
import com.jonquass.guardianhub.core.api.ConfigResponse
import com.jonquass.guardianhub.manager.ConfigManager.SENSITIVE_MASK
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import jakarta.ws.rs.core.Response
import java.time.ZoneId
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(GrizzlyServerExtension::class)
class ConfigResourceIT {

  @Test fun `config resource returns 401 without auth`() {}

  @Test
  fun `config returns 401 without auth`() {
    When { get("/api/config") } Then { statusCode(Response.Status.UNAUTHORIZED.statusCode) }
  }

  @Test
  fun `config returns 200 with auth`() {
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
              body("success", equalTo(true))
              body("token", notNullValue())
            } Extract
            {
              path<String>("token")
            }

    val response =
        Given { header("Authorization", "Bearer $token") } When
            {
              get("/api/config")
            } Then
            {
              statusCode(Response.Status.OK.statusCode)
            } Extract
            {
              `as`(ConfigResponse::class.java)
            }

    assertThat(response.entries).isNotEmpty
    assertThat(response.entries).anyMatch {
      it.key == "LOGIN_PASSWORD" && it.sensitive && it.value == SENSITIVE_MASK
    }
    assertThat(response.entries).anyMatch {
      it.key == "TZ" && it.value == ZoneId.systemDefault().toString()
    }
  }
}
