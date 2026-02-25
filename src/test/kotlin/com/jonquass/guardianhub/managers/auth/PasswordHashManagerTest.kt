package com.jonquass.guardianhub.managers.auth

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PasswordHashManagerTest {
    private val password = "MySecurePassword"

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
}
