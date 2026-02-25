package com.jonquass.guardianhub.managers.auth

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SessionManagerTest {
    @BeforeEach
    fun setup() {
        SessionManager.invalidateSessions()
    }

    @AfterEach
    fun cleanup() {
        SessionManager.invalidateSessions()
    }

    @Test
    fun `createSession should generate unique token`() {
        val token = SessionManager.createSession()

        assertNotNull(token)
        assertTrue(token.isNotEmpty())
        assertTrue(SessionManager.isValidSession(token))
    }

    @Test
    fun `createSession should generate different tokens`() {
        val token1 = SessionManager.createSession()
        val token2 = SessionManager.createSession()

        assertNotNull(token1)
        assertNotNull(token2)
        assertTrue(token1 != token2)
    }

    @Test
    fun `isValidSession should return true for valid unexpired session`() {
        val token = SessionManager.createSession()

        val result = SessionManager.isValidSession(token)

        assertTrue(result)
    }

    @Test
    fun `isValidSession should return false for invalid token`() {
        val invalidToken = "invalid-token-123"

        val result = SessionManager.isValidSession(invalidToken)

        assertFalse(result)
    }

    @Test
    fun `invalidateSession should remove session`() {
        val token = SessionManager.createSession()
        assertTrue(SessionManager.isValidSession(token))

        SessionManager.invalidateSession(token)

        assertFalse(SessionManager.isValidSession(token))
    }

    @Test
    fun `invalidateSessions should remove all sessions`() {
        val token1 = SessionManager.createSession()
        val token2 = SessionManager.createSession()

        assertTrue(SessionManager.isValidSession(token1))
        assertTrue(SessionManager.isValidSession(token2))

        SessionManager.invalidateSessions()

        assertFalse(SessionManager.isValidSession(token1))
        assertFalse(SessionManager.isValidSession(token2))
    }
}
