package com.jonquass.guardianhub.core.manager

import jakarta.ws.rs.core.Response

sealed class Result<out T> {
  data class Success<T>(
      val data: T,
  ) : Result<T>()

  data class Error(
      val message: String,
      val code: Response.Status = Response.Status.INTERNAL_SERVER_ERROR,
  ) : Result<Nothing>()

  val isSuccess
    get() = this is Success

  val isError
    get() = this is Error
}

fun <T> Result<T>.getOrThrow(): T =
    when (this) {
      is Result.Success -> this.data
      is Result.Error -> throw ResultException(this.message, this.code)
    }

fun <T> Result<T>.errOrThrow(): Result.Error =
    when (this) {
      is Result.Success -> throw ResultException("errOrThrow called on Result.Success")
      is Result.Error -> this
    }

class ResultException(
    message: String,
    val code: Response.Status = Response.Status.INTERNAL_SERVER_ERROR,
) : RuntimeException(message)

fun <T> Result<T>.toResponse(): Response =
    when (this) {
      is Result.Success -> Response.ok(this.data).build()
      is Result.Error ->
          Response.status(this.code)
              .entity(mapOf("status" to "error", "message" to this.message))
              .build()
    }
