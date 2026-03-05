package com.jonquass.guardianhub.resource

import com.jonquass.guardianhub.GrizzlyServerExtension
import com.jonquass.guardianhub.core.api.ServiceStatusResponse
import com.jonquass.guardianhub.manager.ServiceStatusManager
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
class ServiceStatusResourceIT {

  @Test
  fun `config returns 401 without auth`() {
    When { get("/api/status/task/ABC-123") } Then
        {
          statusCode(Response.Status.UNAUTHORIZED.statusCode)
        }
  }

  @Test
  fun `config returns empty with random task ID`() {
    val token = GrizzlyServerExtension.loginAndGetToken()

    val response =
        Given {
          contentType(ContentType.JSON)
          header("Authorization", "Bearer $token")
        } When
            {
              get("/api/status/task/FAKE-9999")
            } Then
            {
              statusCode(Response.Status.OK.statusCode)
            } Extract
            {
              body().asString()
            }

    assertThat(response).isEmpty()
  }

  @Test
  fun `config returns task status`() {
    val token = GrizzlyServerExtension.loginAndGetToken()

    val taskId = "ABCD-1234"
    val status =
        ServiceStatusResponse(
            taskId = taskId,
            status = "pending",
            message = "Restart queued",
            progress = 0,
        )

    ServiceStatusManager.tasks[taskId] = status

    val response =
        Given {
          contentType(ContentType.JSON)
          header("Authorization", "Bearer $token")
        } When
            {
              get("/api/status/task/$taskId")
            } Then
            {
              statusCode(Response.Status.OK.statusCode)
            } Extract
            {
              `as`(ServiceStatusResponse::class.java)
            }

    assertThat(response.taskId).isEqualTo(taskId)
  }
}
