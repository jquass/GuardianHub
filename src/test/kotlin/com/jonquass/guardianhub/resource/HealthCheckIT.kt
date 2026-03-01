package com.jonquass.guardianhub.resource

import com.jonquass.guardianhub.GrizzlyServerExtension
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import jakarta.ws.rs.core.Response
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.lessThanOrEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(GrizzlyServerExtension::class)
class HealthCheckIT {

  @Test
  fun `health check returns 200 without auth`() {
    When { get("/api/health") } Then
        {
          statusCode(Response.Status.OK.statusCode)
          body("status", equalTo("healthy"))
          body("timestamp", lessThanOrEqualTo(System.currentTimeMillis()))
        }
  }
}
