package com.jonquass.guardianhub.managers

import com.jonquass.guardianhub.core.api.UpdatePasswordRequest
import com.jonquass.guardianhub.core.config.Env
import com.jonquass.guardianhub.core.manager.errOrThrow
import com.jonquass.guardianhub.core.manager.getOrThrow
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import jakarta.ws.rs.core.Response
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PasswordManagerTest {
    @BeforeEach
    fun setUp() {
        mockkObject(ConfigManager)
        mockkObject(DockerManager)
        every { ConfigManager.upsertConfig(any(), any()) } just runs
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `updatePiholePassword should return success when password is valid and pihole update succeeds`() {
        every { DockerManager.exec(*anyVararg<String>()) } returns true

        val result = PasswordManager.updatePiholePassword(UpdatePasswordRequest("validPassword1!"))

        assertThat(result.isSuccess).isTrue()
        val response = result.getOrThrow()
        assertThat(response.message).isEqualTo("Pi-hole password updated successfully")
        assertThat(response.serviceRestarted).isFalse()
    }

    @Test
    fun `updatePiholePassword should return error when pihole update fails`() {
        every { DockerManager.exec(*anyVararg<String>()) } returns false

        val result = PasswordManager.updatePiholePassword(UpdatePasswordRequest("validPassword1!"))

        assertThat(result.isError).isTrue()
        assertThat(result.errOrThrow().code).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR)
    }

    @Test
    fun `updatePiholePassword should return error when password is invalid`() {
        val result = PasswordManager.updatePiholePassword(UpdatePasswordRequest(""))

        assertThat(result.isError).isTrue()
        assertThat(result.errOrThrow().code).isEqualTo(Response.Status.BAD_REQUEST)
    }

    @Test
    fun `updateNpmPassword should return error when password is blank`() {
        val result = PasswordManager.updateNpmPassword(UpdatePasswordRequest(""))

        assertThat(result.isError).isTrue()
        assertThat(result.errOrThrow().message).isEqualTo("Password cannot be empty")
        assertThat(result.errOrThrow().code).isEqualTo(Response.Status.BAD_REQUEST)
    }

    @Test
    fun `updateNpmPassword should return error when password is less than 8 characters`() {
        val result = PasswordManager.updateNpmPassword(UpdatePasswordRequest("short"))

        assertThat(result.isError).isTrue()
        assertThat(result.errOrThrow().message).isEqualTo("Password must be at least 8 characters")
        assertThat(result.errOrThrow().code).isEqualTo(Response.Status.BAD_REQUEST)
    }

    @Test
    fun `updateNpmPassword should return error when NPM email not configured`() {
        every { ConfigManager.getRawConfigValue(Env.NPM_ADMIN_EMAIL) } returns null
        every { ConfigManager.getRawConfigValue(Env.NPM_ADMIN_PASSWORD) } returns "password"

        val result = PasswordManager.updateNpmPassword(UpdatePasswordRequest("validPassword1!"))

        assertThat(result.isError).isTrue()
        assertThat(result.errOrThrow().code).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR)
    }

    @Test
    fun `updateNpmPassword should return error when NPM password not configured`() {
        every { ConfigManager.getRawConfigValue(Env.NPM_ADMIN_EMAIL) } returns "admin@example.com"
        every { ConfigManager.getRawConfigValue(Env.NPM_ADMIN_PASSWORD) } returns null

        val result = PasswordManager.updateNpmPassword(UpdatePasswordRequest("validPassword1!"))

        assertThat(result.isError).isTrue()
        assertThat(result.errOrThrow().code).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR)
    }

    @Test
    fun `updateWireGuardPassword should return error when password is invalid`() {
        val result = PasswordManager.updateWireGuardPassword(UpdatePasswordRequest(""))

        assertThat(result.isError).isTrue()
        assertThat(result.errOrThrow().code).isEqualTo(Response.Status.BAD_REQUEST)
    }

    @Test
    fun `updateWireGuardPassword should return success when hash and recreate succeed`() {
        every { DockerManager.execWithOutput(*anyVararg<String>()) } returns Pair(0, "PASSWORD_HASH=hashed_value")
        every { DockerManager.recreateContainer(any()) } returns true

        val result = PasswordManager.updateWireGuardPassword(UpdatePasswordRequest("validPassword1!"))

        assertThat(result.isSuccess).isTrue()
        val response = result.getOrThrow()
        assertThat(response.message).isEqualTo("WireGuard password updated successfully")
        assertThat(response.serviceRestarted).isTrue()
    }

    @Test
    fun `updateWireGuardPassword should return success with serviceRestarted false when recreate fails`() {
        every { DockerManager.execWithOutput(*anyVararg<String>()) } returns Pair(0, "PASSWORD_HASH=hashed_value")
        every { DockerManager.recreateContainer(any()) } returns false

        val result = PasswordManager.updateWireGuardPassword(UpdatePasswordRequest("validPassword1!"))

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow().serviceRestarted).isFalse()
    }

    @Test
    fun `updateWireGuardPassword should return error when hash generation fails`() {
        every { DockerManager.execWithOutput(*anyVararg<String>()) } returns Pair(1, null)

        val result = PasswordManager.updateWireGuardPassword(UpdatePasswordRequest("validPassword1!"))

        assertThat(result.isError).isTrue()
        assertThat(result.errOrThrow().message).isEqualTo("Failed to generate WireGuard password hash")
        assertThat(result.errOrThrow().code).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR)
    }

    @Test
    fun `updateWireGuardPassword should return error when hash output cannot be parsed`() {
        every { DockerManager.execWithOutput(*anyVararg<String>()) } returns Pair(0, "UNEXPECTED_OUTPUT=something")

        val result = PasswordManager.updateWireGuardPassword(UpdatePasswordRequest("validPassword1!"))

        assertThat(result.isError).isTrue()
        assertThat(result.errOrThrow().message).isEqualTo("Failed to generate WireGuard password hash")
    }

    @Test
    fun `updateNpmPassword should return error when token fetch fails`() {
        every { ConfigManager.getRawConfigValue(Env.NPM_ADMIN_EMAIL) } returns "admin@example.com"
        every { ConfigManager.getRawConfigValue(Env.NPM_ADMIN_PASSWORD) } returns "currentPassword1!"
        every { DockerManager.execWithOutput(*anyVararg<String>()) } returns Pair(1, null)

        val result = PasswordManager.updateNpmPassword(UpdatePasswordRequest("validPassword1!"))

        assertThat(result.isError).isTrue()
        assertThat(result.errOrThrow().code).isEqualTo(Response.Status.UNAUTHORIZED)
    }

    @Test
    fun `updateNpmPassword should return error when token is not present in response`() {
        every { ConfigManager.getRawConfigValue(Env.NPM_ADMIN_EMAIL) } returns "admin@example.com"
        every { ConfigManager.getRawConfigValue(Env.NPM_ADMIN_PASSWORD) } returns "currentPassword1!"
        every { DockerManager.execWithOutput(*anyVararg<String>()) } returns Pair(0, """{"error":"invalid credentials"}""")

        val result = PasswordManager.updateNpmPassword(UpdatePasswordRequest("validPassword1!"))

        assertThat(result.isError).isTrue()
        assertThat(result.errOrThrow().code).isEqualTo(Response.Status.UNAUTHORIZED)
    }

    @Test
    fun `updateNpmPassword should return error when user id fetch fails`() {
        every { ConfigManager.getRawConfigValue(Env.NPM_ADMIN_EMAIL) } returns "admin@example.com"
        every { ConfigManager.getRawConfigValue(Env.NPM_ADMIN_PASSWORD) } returns "currentPassword1!"
        every { DockerManager.execWithOutput(*anyVararg<String>()) } returnsMany
            listOf(
                Pair(0, """{"token":"abc123"}"""), // token fetch succeeds
                Pair(1, null), // user id fetch fails
            )

        val result = PasswordManager.updateNpmPassword(UpdatePasswordRequest("validPassword1!"))

        assertThat(result.isError).isTrue()
        assertThat(result.errOrThrow().code).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR)
    }

    @Test
    fun `updateNpmPassword should return error when password update api returns false`() {
        every { ConfigManager.getRawConfigValue(Env.NPM_ADMIN_EMAIL) } returns "admin@example.com"
        every { ConfigManager.getRawConfigValue(Env.NPM_ADMIN_PASSWORD) } returns "currentPassword1!"
        every { DockerManager.execWithOutput(*anyVararg<String>()) } returnsMany
            listOf(
                Pair(0, """{"token":"abc123"}"""), // token fetch
                Pair(0, """[{"id":1,"email":"admin@example.com"}]"""), // user id fetch
                Pair(0, "false"), // password update
            )

        val result = PasswordManager.updateNpmPassword(UpdatePasswordRequest("validPassword1!"))

        assertThat(result.isError).isTrue()
        assertThat(result.errOrThrow().message).isEqualTo("Failed to update NPM password via API")
    }

    @Test
    fun `updateNpmPassword should return success when all steps succeed`() {
        every { ConfigManager.getRawConfigValue(Env.NPM_ADMIN_EMAIL) } returns "admin@example.com"
        every { ConfigManager.getRawConfigValue(Env.NPM_ADMIN_PASSWORD) } returns "currentPassword1!"
        every { DockerManager.execWithOutput(*anyVararg<String>()) } returnsMany
            listOf(
                Pair(0, """{"token":"abc123"}"""), // token fetch
                Pair(0, """[{"id":1,"email":"admin@example.com"}]"""), // user id fetch
                Pair(0, "true"), // password update
            )

        val result = PasswordManager.updateNpmPassword(UpdatePasswordRequest("validPassword1!"))

        assertThat(result.isSuccess).isTrue()
        val response = result.getOrThrow()
        assertThat(response.message).isEqualTo("NPM password updated successfully")
        assertThat(response.serviceRestarted).isFalse()
    }
}
