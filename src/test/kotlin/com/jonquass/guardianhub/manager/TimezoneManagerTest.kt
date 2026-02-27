package com.jonquass.guardianhub.manager

import com.jonquass.guardianhub.core.api.UpdateTimezoneRequest
import com.jonquass.guardianhub.core.errOrThrow
import com.jonquass.guardianhub.core.getOrThrow
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
class TimezoneManagerTest {

  @BeforeEach
  fun setUp() {
    mockkObject(ConfigManager)
    mockkObject(ServiceStatusManager)
    every { ConfigManager.upsertConfig(any(), any()) } just runs
  }

  @AfterEach
  fun tearDown() {
    unmockkAll()
  }

  // --- isValidTimezone ---

  @Test
  fun `isValidTimezone should return true for valid timezone`() {
    assertThat(TimezoneManager.isValidTimezone("America/New_York")).isTrue()
  }

  @Test
  fun `isValidTimezone should return true for UTC`() {
    assertThat(TimezoneManager.isValidTimezone("UTC")).isTrue()
  }

  @Test
  fun `isValidTimezone should return false for invalid timezone`() {
    assertThat(TimezoneManager.isValidTimezone("Not/A/Timezone")).isFalse()
  }

  @Test
  fun `isValidTimezone should return false for blank string`() {
    assertThat(TimezoneManager.isValidTimezone("")).isFalse()
  }

  // --- getTimezones ---

  @Test
  fun `getTimezones should return success with sorted timezones`() {
    val result = TimezoneManager.getTimezonesResult()

    assertThat(result.isSuccess).isTrue()
    val body = result.getOrThrow()

    assertThat(body["status"]).isEqualTo("success")

    @Suppress("UNCHECKED_CAST") val timezones = body["timezones"] as List<String>
    assertThat(timezones).isNotEmpty()
    assertThat(timezones).contains("UTC", "America/New_York")
    assertThat(timezones).isSortedAccordingTo(compareBy { it })
  }

  // --- updateTimezones ---

  @Test
  fun `updateTimezones should return error for invalid timezone`() {
    val result =
        TimezoneManager.updateTimezonesResult(UpdateTimezoneRequest(timezone = "Fake/Timezone"))

    assertThat(result.isError).isTrue()
    val error = result.errOrThrow()
    assertThat(error.message).contains("Fake/Timezone")
    assertThat(error.code).isEqualTo(Response.Status.BAD_REQUEST)
  }

  @Test
  fun `updateTimezones should return success for valid timezone`() {
    every { ServiceStatusManager.restartServicesAsync(any()) } returns "test-task-id"

    val result =
        TimezoneManager.updateTimezonesResult(UpdateTimezoneRequest(timezone = "America/New_York"))

    assertThat(result.isSuccess).isTrue()
    val body = result.getOrThrow()
    assertThat(body["taskId"]).isEqualTo("test-task-id")
    assertThat(body["message"] as String).contains("America/New_York")
  }

  @Test
  fun `updateTimezones should return error when restartServicesAsync throws`() {
    every { ServiceStatusManager.restartServicesAsync(any()) } throws
        RuntimeException("Docker error")

    val result =
        TimezoneManager.updateTimezonesResult(UpdateTimezoneRequest(timezone = "America/New_York"))

    assertThat(result.isError).isTrue()
    assertThat(result.errOrThrow().code).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR)
  }
}
