package com.jonquass.guardianhub.resource

import com.jonquass.guardianhub.GrizzlyServerExtension
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import jakarta.ws.rs.core.Response
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(GrizzlyServerExtension::class)
class ServiceStatusResourceIT {

  @Test
  fun `config returns 401 without auth`() {
    When { get("/api/status/task/ABC-123") } Then
        {
          statusCode(Response.Status.UNAUTHORIZED.statusCode)
        }
  }
}
