package com.jonquass.guardianhub.managers

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthManagerTest {

    @BeforeEach
    fun setup() {
        // Clear any existing sessions
        AuthManager.sessions.clear()
    }

    @AfterEach
    fun cleanup() {
        AuthManager.sessions.clear()
    }

    @Test
    fun `createSession should generate unique token`() {
        // When: Creating a session
        val token = AuthManager.createSession()

        // Then: Token should not be empty
        assertNotNull(token)
        assertTrue(token.isNotEmpty())

        // And: Session should be stored
        assertTrue(AuthManager.sessions.containsKey(token))
    }

    @Test
    fun `createSession should generate different tokens`() {
        // When: Creating multiple sessions
        val token1 = AuthManager.createSession()
        val token2 = AuthManager.createSession()

        // Then: Tokens should be different
        assertNotNull(token1)
        assertNotNull(token2)
        assertTrue(token1 != token2)
    }

    @Test
    fun `isValidSession should return true for valid unexpired session`() {
        // Given: A valid session
        val token = AuthManager.createSession()

        // When: Checking if session is valid
        val result = AuthManager.isValidSession(token)

        // Then: Should return true
        assertTrue(result)
    }

    @Test
    fun `isValidSession should return false for invalid token`() {
        // Given: An invalid token
        val invalidToken = "invalid-token-123"

        // When: Checking if session is valid
        val result = AuthManager.isValidSession(invalidToken)

        // Then: Should return false
        assertFalse(result)
    }

    @Test
    fun `invalidateSession should remove session`() {
        // Given: A valid session
        val token = AuthManager.createSession()
        assertTrue(AuthManager.isValidSession(token))

        // When: Invalidating the session
        AuthManager.invalidateSession(token)

        // Then: Session should no longer be valid
        assertFalse(AuthManager.isValidSession(token))
    }

    @Test
    fun `hashPassword should create valid bcrypt hash`() {
        // Given: A plain text password
        val password = "MySecurePassword123"

        // When: Hashing the password
        val hash = AuthManager.hashPassword(password)

        // Then: Hash should start with bcrypt prefix
        assertTrue(hash.startsWith("\$2a\$") || hash.startsWith("\$2b\$"))

        // And: Hash should be proper length (60 characters)
        assertTrue(hash.length == 60)
    }

    @Test
    fun `hashPassword should create different hashes for same password`() {
        // Given: Same password hashed twice
        val password = "TestPassword123"

        // When: Hashing multiple times
        val hash1 = AuthManager.hashPassword(password)
        val hash2 = AuthManager.hashPassword(password)

        // Then: Hashes should be different (due to salt)
        assertTrue(hash1 != hash2)

        // But both should be valid bcrypt hashes
        assertTrue(hash1.startsWith("\$2a\$") || hash1.startsWith("\$2b\$"))
        assertTrue(hash2.startsWith("\$2a\$") || hash2.startsWith("\$2b\$"))
    }
}