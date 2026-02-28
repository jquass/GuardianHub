package com.jonquass.guardianhub.core

import com.jonquass.guardianhub.core.exception.ResultException
import jakarta.ws.rs.core.Response
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ResultTest {

  // --- isSuccess / isError ---

  @Test
  fun `Success isSuccess should be true`() {
    val result = Result.Success("data")
    assertThat(result.isSuccess).isTrue()
    assertThat(result.isError).isFalse()
  }

  @Test
  fun `Error isError should be true`() {
    val result = Result.Error("something went wrong")
    assertThat(result.isError).isTrue()
    assertThat(result.isSuccess).isFalse()
  }

  // --- Error defaults ---

  @Test
  fun `Error should default to INTERNAL_SERVER_ERROR status`() {
    val result = Result.Error("something went wrong")
    assertThat(result.code).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR)
  }

  @Test
  fun `Error should use provided status code`() {
    val result = Result.Error("not found", Response.Status.BAD_REQUEST)
    assertThat(result.code).isEqualTo(Response.Status.BAD_REQUEST)
  }

  // --- getOrThrow ---

  @Test
  fun `getOrThrow should return data on Success`() {
    val result = Result.Success("hello")
    assertThat(result.getOrThrow()).isEqualTo("hello")
  }

  @Test
  fun `getOrThrow should return null data on Success with null`() {
    val result = Result.Success<Nothing?>(null)
    val data = result.getOrThrow()
    assertThat(data as Any?).isNull()
  }

  @Test
  fun `getOrThrow should throw ResultException on Error`() {
    val result = Result.Error("something went wrong", Response.Status.BAD_REQUEST)
    assertThatThrownBy { result.getOrThrow() }
        .isInstanceOf(ResultException::class.java)
        .hasMessage("something went wrong")
  }

  // --- errOrThrow ---

  @Test
  fun `errOrThrow should return Error on Error`() {
    val result = Result.Error("something went wrong", Response.Status.BAD_REQUEST)
    val error = result.errOrThrow()
    assertThat(error.message).isEqualTo("something went wrong")
    assertThat(error.code).isEqualTo(Response.Status.BAD_REQUEST)
  }

  @Test
  fun `errOrThrow should throw ResultException on Success`() {
    val result = Result.Success("data")
    assertThatThrownBy { result.errOrThrow() }
        .isInstanceOf(ResultException::class.java)
        .hasMessage("errOrThrow called on Result.Success")
  }

  // --- toResponse ---

  @Test
  fun `toResponse should return 200 with data on Success`() {
    val result = Result.Success("response data")
    val response = result.toResponse()
    assertThat(response.status).isEqualTo(Response.Status.OK.statusCode)
    assertThat(response.entity).isEqualTo("response data")
  }

  @Test
  fun `toResponse should return 500 with error body on Error with default status`() {
    val result = Result.Error("something went wrong")
    val response = result.toResponse()

    assertThat(response.status).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.statusCode)

    @Suppress("UNCHECKED_CAST") val body = response.entity as Map<String, String>
    assertThat(body["status"]).isEqualTo("error")
    assertThat(body["message"]).isEqualTo("something went wrong")
  }

  @Test
  fun `toResponse should return correct status code on Error with custom status`() {
    val result = Result.Error("bad input", Response.Status.BAD_REQUEST)
    val response = result.toResponse()

    assertThat(response.status).isEqualTo(Response.Status.BAD_REQUEST.statusCode)

    @Suppress("UNCHECKED_CAST") val body = response.entity as Map<String, String>
    assertThat(body["status"]).isEqualTo("error")
    assertThat(body["message"]).isEqualTo("bad input")
  }
}
