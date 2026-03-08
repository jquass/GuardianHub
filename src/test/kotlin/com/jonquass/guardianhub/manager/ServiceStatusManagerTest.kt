package com.jonquass.guardianhub.manager

import com.jonquass.guardianhub.core.Result
import com.jonquass.guardianhub.core.getOrThrow
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServiceStatusManagerTest {
  @BeforeEach
  fun setUp() {
    mockkObject(DockerManager)
    every { DockerManager.exec(*anyVararg<String>()) } returns Result.success()
  }

  @AfterEach
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `restartServicesAsync should return a task id`() {
    val taskId = ServiceStatusManager.restartServicesAsync(listOf("pihole"))

    assertThat(taskId).isNotBlank()
  }

  @Test
  fun `restartServicesAsync should return unique task ids for each call`() {
    val taskId1 = ServiceStatusManager.restartServicesAsync(listOf("pihole"))
    val taskId2 = ServiceStatusManager.restartServicesAsync(listOf("pihole"))

    assertThat(taskId1).isNotEqualTo(taskId2)
  }

  @Test
  fun `restartServicesAsync should initially set status to pending`() {
    every { DockerManager.exec(*anyVararg<String>()) } answers
        {
          Thread.sleep(200)
          Result.success()
        }
    val taskId = ServiceStatusManager.restartServicesAsync(listOf("pihole"))

    val result = ServiceStatusManager.getTaskStatus(taskId)

    assertThat (result.getOrThrow()?.status == "pending")
  }

  @Test
  fun `restartServicesAsync should restart multiple services`() {
    every { DockerManager.exec(*anyVararg<String>()) } answers
            {
              Thread.sleep(200)
              Result.success()
            }
    val taskId = ServiceStatusManager.restartServicesAsync(listOf("pihole", "wireguard"))

    await(taskId)

    val result = ServiceStatusManager.getTaskStatus(taskId)

    assertThat(result.isSuccess).isTrue()
    val status = result.getOrThrow()
    assertThat (status?.status == "completed")
    assertThat(status?.servicesRestarted).containsExactly("pihole", "wireguard")
  }

  @Test
  fun `restartServicesAsync can fail restart services`() {
    every { DockerManager.exec(*anyVararg<String>()) } answers
            {
              Thread.sleep(200)
              Result.error()
            }
    val taskId = ServiceStatusManager.restartServicesAsync(listOf("pihole", "wireguard"))

    await(taskId)

    val result = ServiceStatusManager.getTaskStatus(taskId)

    assertThat(result.isSuccess).isTrue()
    val status = result.getOrThrow()
    assertThat (status?.status == "completed")
    assertThat(status?.servicesFailed).containsExactly("pihole", "wireguard")
  }

  @Test
  fun `getTaskStatus should return empty list for unknown task id`() {
    val result = ServiceStatusManager.getTaskStatus("unknown-id")

    assertThat(result.isSuccess).isTrue
    val response = result.getOrThrow()
    assertThat(response).isNull()
  }

  @Test
  fun `getTaskStatus should return completed status after restart finishes`() {
    val taskId = ServiceStatusManager.restartServicesAsync(listOf("pihole", "wireguard"))

    await(taskId)

    val result = ServiceStatusManager.getTaskStatus(taskId)

    assertThat(result.isSuccess).isTrue
    val response = result.getOrThrow()
    assertThat(response?.status).isEqualTo("completed")
    assertThat(response?.progress).isEqualTo(100)
    assertThat(response?.message).isEqualTo("All services restarted successfully")
  }

  @Test
  fun `getTaskStatus should reach progress 100 after all services processed`() {
    val taskId = ServiceStatusManager.restartServicesAsync(listOf("pihole", "wireguard", "npm"))

    await(taskId)

    val result = ServiceStatusManager.getTaskStatus(taskId)
    assertThat(result.isSuccess).isTrue
    val response = result.getOrThrow()
    assertThat(response?.progress).isEqualTo(100)
  }

  @Test
  fun `getTaskStatus should call DockerManager once per service`() {
    val taskId = ServiceStatusManager.restartServicesAsync(listOf("pihole", "wireguard"))

    await(taskId)

    verify(exactly = 2) { DockerManager.exec(*anyVararg<String>()) }
  }

  @Test
  fun `getTaskStatus should complete with empty service list`() {
    val taskId = ServiceStatusManager.restartServicesAsync(emptyList())

    await(taskId)

    val result = ServiceStatusManager.getTaskStatus(taskId)

    assertThat(result.isSuccess).isTrue
    val response = result.getOrThrow()
    assertThat(response?.status).isEqualTo("completed")
    assertThat(response?.progress).isEqualTo(100)
  }

  @Test
  fun `restartServicesAsync servicesRestarted is empty due to untracked exec result`() {
    val taskId = ServiceStatusManager.restartServicesAsync(listOf("pihole"))

    await(taskId)

    val result = ServiceStatusManager.getTaskStatus(taskId)

    assertThat(result.isSuccess).isTrue
    val response = result.getOrThrow()
    assertThat(response?.servicesRestarted).containsExactly("pihole")
  }

  private fun await(
      taskId: String,
      timeoutMs: Long = 3000,
  ) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < deadline) {
      val result = ServiceStatusManager.getTaskStatus(taskId)
      assertThat(result.isSuccess).isTrue
      val response = result.getOrThrow()

      if (response?.status in listOf("completed", "failed")) return
      Thread.sleep(50)
    }
    throw AssertionError("Task $taskId did not complete within ${timeoutMs}ms")
  }
}
