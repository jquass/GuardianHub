package com.jonquass.guardianhub.validator

import com.jonquass.guardianhub.core.errOrThrow
import jakarta.ws.rs.core.Response
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PasswordValidatorTest {

    @Test
    fun `validate should return success for valid password`() {
        val result = PasswordValidator.validate("securePassword123")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `validate should return error for blank password`() {
        val result = PasswordValidator.validate("")

        assertTrue(result.isError)
        val error = result.errOrThrow()
        assertEquals("Password must not be blank", error.message)
        assertEquals(Response.Status.BAD_REQUEST, error.code)
    }

    @Test
    fun `validate should return error for whitespace-only password`() {
        val result = PasswordValidator.validate("       ")

        assertTrue(result.isError)
        val error = result.errOrThrow()
        assertEquals("Password must not be blank", error.message)
        assertEquals(Response.Status.BAD_REQUEST, error.code)
    }

    @Test
    fun `validate should return error for password shorter than 8 characters`() {
        val result = PasswordValidator.validate("short")

        assertTrue(result.isError)
        val error = result.errOrThrow()
        assertEquals("Password must be at least 8 characters long", error.message)
        assertEquals(Response.Status.BAD_REQUEST, error.code)
    }

    @Test
    fun `validate should return error for password of exactly 7 characters`() {
        val result = PasswordValidator.validate("1234567")

        assertTrue(result.isError)
        val error = result.errOrThrow()
        assertEquals(Response.Status.BAD_REQUEST, error.code)
    }

    @Test
    fun `validate should return success for password of exactly 8 characters`() {
        val result = PasswordValidator.validate("12345678")

        assertTrue(result.isSuccess)
    }
}