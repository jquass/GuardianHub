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
}
