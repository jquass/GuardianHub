package com.jonquass.guardianhub.core

import com.jonquass.guardianhub.core.Result.Companion.DEFAULT_ERROR
import com.jonquass.guardianhub.core.api.ErrorResponse
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
    val result = Result.success("data")
    assertThat(result.isSuccess).isTrue()
    assertThat(result.isError).isFalse()
  }

  @Test
  fun `Error isError should be true`() {
    val result = Result.error("something went wrong")
    assertThat(result.isError).isTrue()
    assertThat(result.isSuccess).isFalse()
  }

  // --- Error defaults ---

  @Test
  fun `Error should default to INTERNAL_SERVER_ERROR status`() {
    val result = Result.error("something went wrong")
    assertThat(result.errOrThrow().code).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR)
  }

  @Test
  fun `Error should use provided status code`() {
    val result = Result.error("not found", Response.Status.BAD_REQUEST)
    assertThat(result.errOrThrow().code).isEqualTo(Response.Status.BAD_REQUEST)
  }

  // --- getOrThrow ---

  @Test
  fun `getOrThrow should return data on Success`() {
    val result = Result.success("hello")
    assertThat(result.getOrThrow()).isEqualTo("hello")
  }

  @Test
  fun `getOrThrow should return Unit on Success by default`() {
    val result = Result.success()
    val data = result.getOrThrow()
    assertThat(data).isInstanceOf(Unit::class.java)
  }

  @Test
  fun `getOrThrow should throw ResultException on Error`() {
    val result = Result.error("something went wrong", Response.Status.BAD_REQUEST)
    assertThatThrownBy { result.getOrThrow() }
        .isInstanceOf(ResultException::class.java)
        .hasMessage("something went wrong")
  }

  @Test
  fun `getOrThrow should throw ResultException on Error with defaults`() {
    val result = Result.error()
    assertThatThrownBy { result.getOrThrow() }
        .isInstanceOf(ResultException::class.java)
        .hasMessage(DEFAULT_ERROR)
  }

  // --- errOrThrow ---

  @Test
  fun `errOrThrow should return Error on Error`() {
    val result = Result.error("something went wrong", Response.Status.BAD_REQUEST)
    val error = result.errOrThrow()
    assertThat(error.message).isEqualTo("something went wrong")
    assertThat(error.code).isEqualTo(Response.Status.BAD_REQUEST)
  }

  @Test
  fun `errOrThrow should throw ResultException on Success`() {
    val result = Result.success("data")
    assertThatThrownBy { result.errOrThrow() }
        .isInstanceOf(ResultException::class.java)
        .hasMessage("errOrThrow called on Result.Success")
  }

  // --- toResponse ---

  @Test
  fun `toResponse should return 200 with data on Success`() {
    val result = Result.success("response data")
    val response = result.toResponse()
    assertThat(response.status).isEqualTo(Response.Status.OK.statusCode)
    assertThat(response.entity).isEqualTo("response data")
  }

  @Test
  fun `toResponse should return 500 with error body on Error with default status`() {
    val result = Result.error("something went wrong")
    val response = result.toResponse()

    assertThat(response.status).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.statusCode)

    val errorResponse = response.entity as ErrorResponse
    assertThat(errorResponse.message).isEqualTo("something went wrong")
  }

  @Test
  fun `toResponse should return 500 with error body on Error with default body`() {
    val result = Result.error()
    val response = result.toResponse()

    assertThat(response.status).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.statusCode)

    val errorResponse = response.entity as ErrorResponse
    assertThat(errorResponse.message).isEqualTo(DEFAULT_ERROR)
  }

  @Test
  fun `toResponse should return correct status code on Error with custom status`() {
    val message = "bad input"
    val result = Result.error(message, Response.Status.BAD_REQUEST)
    val response = result.toResponse()

    assertThat(response.status).isEqualTo(Response.Status.BAD_REQUEST.statusCode)

    val errorResponse = response.entity as ErrorResponse
    assertThat(errorResponse.message).isEqualTo(message)
  }

  // --- orError ---

  @Test
  fun `orError should return Success when string is not null or empty`() {
    val result = "valid-token".orError("Missing token")
    assertThat(result.isSuccess).isTrue()
    assertThat((result as Result.Success).data).isEqualTo("valid-token")
  }

  @Test
  fun `orError should return Error when string is null`() {
    val result = null.orError("Missing token", Response.Status.UNAUTHORIZED)
    assertThat(result.isError).isTrue()
    val error = result as Result.Error
    assertThat(error.message).isEqualTo("Missing token")
    assertThat(error.code).isEqualTo(Response.Status.UNAUTHORIZED)
  }

  @Test
  fun `orError should return Error when string is empty`() {
    val result = "".orError("Missing token", Response.Status.UNAUTHORIZED)
    assertThat(result.isError).isTrue()
    val error = result as Result.Error
    assertThat(error.message).isEqualTo("Missing token")
    assertThat(error.code).isEqualTo(Response.Status.UNAUTHORIZED)
  }

  @Test
  fun `orError should use default status code when not provided`() {
    val result = null.orError("Missing token")
    val error = result as Result.Error
    assertThat(error.code).isEqualTo(Response.Status.UNAUTHORIZED)
  }

  @Test
  fun `orError should use default status code and message when not provided`() {
    val result = null.orError()
    val error = result as Result.Error
    assertThat(error.code).isEqualTo(Response.Status.UNAUTHORIZED)
    assertThat(error.message).isNull()
  }
}
