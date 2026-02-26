package com.jonquass.guardianhub.managers.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.ConcurrentHashMap

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PasswordHashManagerTest {
    private val password = "MySecurePassword"

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    private fun insertExpiredSession(token: String) {
        val sessionsField = SessionManager::class.java.getDeclaredField("sessions")
        sessionsField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val sessions = sessionsField.get(SessionManager) as ConcurrentHashMap<String, Long>
        sessions[token] = System.currentTimeMillis() - 1000L // already expired
    }

    @Test
    fun `hashPassword should create valid bcrypt hash`() {
        val hash = PasswordHashManager.hashPassword(password)

        assertTrue(hash.startsWith("\$2a\$") || hash.startsWith("\$2b\$"))
        assertTrue(hash.length == 60)
    }

    @Test
    fun `hashPassword should create different hashes for same password`() {
        val hash1 = PasswordHashManager.hashPassword(password)
        val hash2 = PasswordHashManager.hashPassword(password)

        assertTrue(hash1 != hash2)
        assertTrue(hash1.startsWith("\$2a\$") || hash1.startsWith("\$2b\$"))
        assertTrue(hash2.startsWith("\$2a\$") || hash2.startsWith("\$2b\$"))
    }

    @Test
    fun `verifyHash should work on hashed password`() {
        val hash = PasswordHashManager.hashPassword(password)

        val validHash = PasswordHashManager.verifyHash(password, hash)

        assertTrue(validHash)
    }

    @Test
    fun `verifyHash should not work on different passwords`() {
        val hash = PasswordHashManager.hashPassword(password)

        val validHash = PasswordHashManager.verifyHash("AnotherPassword", hash)

        assertFalse(validHash)
    }

    @Test
    fun `verifyHash should return false and swallow exception when BCrypt verifyer throws`() {
        mockkStatic(BCrypt::class)
        every { BCrypt.verifyer() } throws RuntimeException("Simulated BCrypt failure")

        val result = PasswordHashManager.verifyHash("anyPassword", "\$2a\$10\$invalidhash")

        assertFalse(result)
    }

    @Test
    fun `verifyHash should return false when hash is malformed and causes exception`() {
        // Malformed hash that causes BCrypt to throw internally without mocking
        val result = PasswordHashManager.verifyHash("anyPassword", "not-a-valid-bcrypt-hash")

        assertFalse(result)
    }

    @Test
    fun `hashPassword should rethrow the original exception BCrypt throws`() {
        mockkStatic(BCrypt::class)
        every { BCrypt.withDefaults() } throws IllegalStateException("Illegal state")

        val exception =
            assertThrows<IllegalStateException> {
                PasswordHashManager.hashPassword("anyPassword")
            }

        assert(exception.message == "Illegal state")
    }
}
