package com.jonquass.guardianhub.resource

import com.jonquass.guardianhub.GrizzlyServerExtension
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(GrizzlyServerExtension::class)
class HealthCheckIT {

  @Test
  fun `health check returns 200 without auth`() {
    When { get("/health") } Then
        {
          statusCode(200)
          body("status", equalTo("healthy"))
          body("service", equalTo("Guardian Hub Config UI"))
          body("version", equalTo("1.0.0"))
        }
  }
}
