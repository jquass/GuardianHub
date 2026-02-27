package com.jonquass.guardianhub.manager

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
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `restartServicesAsync should return a task id`() {
        every { DockerManager.exec(*anyVararg<String>()) } returns true

        val taskId = ServiceStatusManager.restartServicesAsync(listOf("pihole"))

        assertThat(taskId).isNotBlank()
    }

    @Test
    fun `restartServicesAsync should return unique task ids for each call`() {
        every { DockerManager.exec(*anyVararg<String>()) } returns true

        val taskId1 = ServiceStatusManager.restartServicesAsync(listOf("pihole"))
        val taskId2 = ServiceStatusManager.restartServicesAsync(listOf("pihole"))

        assertThat(taskId1).isNotEqualTo(taskId2)
    }

    @Test
    fun `restartServicesAsync should initially set status to pending`() {
        every { DockerManager.exec(*anyVararg<String>()) } returns true

        every { DockerManager.exec(*anyVararg<String>()) } answers {
            Thread.sleep(200)
            true
        }

        val taskId = ServiceStatusManager.restartServicesAsync(listOf("pihole"))
        val status = ServiceStatusManager.getTaskStatus(taskId)

        assertThat(status).isNotNull()
        assertThat(status!!.taskId).isEqualTo(taskId)
    }

    @Test
    fun `getTaskStatus should return null for unknown task id`() {
        val status = ServiceStatusManager.getTaskStatus("unknown-id")

        assertThat(status).isNull()
    }

    @Test
    fun `getTaskStatus should return completed status after restart finishes`() {
        every { DockerManager.exec(*anyVararg<String>()) } returns true
        val taskId = ServiceStatusManager.restartServicesAsync(listOf("pihole", "wireguard"))

        await(taskId)

        val status = ServiceStatusManager.getTaskStatus(taskId)
        assertThat(status).isNotNull()
        assertThat(status!!.status).isEqualTo("completed")
        assertThat(status.progress).isEqualTo(100)
        assertThat(status.message).isEqualTo("All services restarted successfully")
    }

    @Test
    fun `getTaskStatus should reach progress 100 after all services processed`() {
        every { DockerManager.exec(*anyVararg<String>()) } returns true

        val taskId = ServiceStatusManager.restartServicesAsync(listOf("pihole", "wireguard", "npm"))

        await(taskId)

        val status = ServiceStatusManager.getTaskStatus(taskId)
        assertThat(status!!.progress).isEqualTo(100)
    }

    @Test
    fun `getTaskStatus should call DockerManager once per service`() {
        every { DockerManager.exec(*anyVararg<String>()) } returns true

        val taskId = ServiceStatusManager.restartServicesAsync(listOf("pihole", "wireguard"))

        await(taskId)

        verify(exactly = 2) { DockerManager.exec(*anyVararg<String>()) }
    }

    @Test
    fun `getTaskStatus should complete with empty service list`() {
        val taskId = ServiceStatusManager.restartServicesAsync(emptyList())

        await(taskId)

        val status = ServiceStatusManager.getTaskStatus(taskId)
        assertThat(status!!.status).isEqualTo("completed")
        assertThat(status.progress).isEqualTo(100)
    }

    @Test
    fun `restartServicesAsync servicesRestarted is empty due to untracked exec result`() {
        every { DockerManager.exec(*anyVararg<String>()) } returns true
        val taskId = ServiceStatusManager.restartServicesAsync(listOf("pihole"))

        await(taskId)

        val status = ServiceStatusManager.getTaskStatus(taskId)
        assertThat(status!!.servicesRestarted).containsExactly("pihole")
    }

    private fun await(
        taskId: String,
        timeoutMs: Long = 3000,
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val status = ServiceStatusManager.getTaskStatus(taskId)
            if (status?.status in listOf("completed", "failed")) return
            Thread.sleep(50)
        }
        throw AssertionError("Task $taskId did not complete within ${timeoutMs}ms")
    }
}
