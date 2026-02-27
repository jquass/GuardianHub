package com.jonquass.guardianhub.manager

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import java.io.ByteArrayInputStream
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DockerManagerTest {

  @AfterEach
  fun tearDown() {
    unmockkAll()
  }

  // --- exec ---

  @Test
  fun `exec should return true when process exits with code 0`() {
    mockkConstructor(ProcessBuilder::class)
    every { anyConstructed<ProcessBuilder>().redirectErrorStream(true) } returns
        mockk(relaxed = true) { every { start() } returns mockk { every { waitFor() } returns 0 } }

    assertThat(DockerManager.exec("ps")).isTrue()
  }

  @Test
  fun `exec should return false when process exits with non-zero code`() {
    mockkConstructor(ProcessBuilder::class)
    every { anyConstructed<ProcessBuilder>().redirectErrorStream(true) } returns
        mockk(relaxed = true) { every { start() } returns mockk { every { waitFor() } returns 1 } }

    assertThat(DockerManager.exec("ps")).isFalse()
  }

  @Test
  fun `exec should return false when process throws exception`() {
    mockkConstructor(ProcessBuilder::class)
    every { anyConstructed<ProcessBuilder>().redirectErrorStream(true) } returns
        mockk(relaxed = true) { every { start() } throws RuntimeException("docker not found") }

    assertThat(DockerManager.exec("ps")).isFalse()
  }

  // --- execWithOutput ---

  @Test
  fun `execWithOutput should return exit code and output on success`() {
    mockkConstructor(ProcessBuilder::class)
    every { anyConstructed<ProcessBuilder>().redirectErrorStream(true) } returns
        mockk(relaxed = true) {
          every { start() } returns
              mockk {
                every { inputStream } returns ByteArrayInputStream("some output".toByteArray())
                every { waitFor() } returns 0
              }
        }

    val (exitCode, output) = DockerManager.execWithOutput("ps")

    assertThat(exitCode).isEqualTo(0)
    assertThat(output).isEqualTo("some output")
  }

  @Test
  fun `execWithOutput should return non-zero exit code and output on failure`() {
    mockkConstructor(ProcessBuilder::class)
    every { anyConstructed<ProcessBuilder>().redirectErrorStream(true) } returns
        mockk(relaxed = true) {
          every { start() } returns
              mockk {
                every { inputStream } returns ByteArrayInputStream("error output".toByteArray())
                every { waitFor() } returns 1
              }
        }

    val (exitCode, output) = DockerManager.execWithOutput("ps")

    assertThat(exitCode).isEqualTo(1)
    assertThat(output).isEqualTo("error output")
  }

  @Test
  fun `execWithOutput should return -1 and null when exception is thrown`() {
    mockkConstructor(ProcessBuilder::class)
    every { anyConstructed<ProcessBuilder>().redirectErrorStream(true) } returns
        mockk(relaxed = true) { every { start() } throws RuntimeException("docker not found") }

    val (exitCode, output) = DockerManager.execWithOutput("ps")

    assertThat(exitCode).isEqualTo(-1)
    assertThat(output).isNull()
  }

  @Test
  fun `execWithOutput should return null output when process produces no output`() {
    mockkConstructor(ProcessBuilder::class)
    every { anyConstructed<ProcessBuilder>().redirectErrorStream(true) } returns
        mockk(relaxed = true) {
          every { start() } returns
              mockk {
                every { inputStream } returns ByteArrayInputStream(ByteArray(0))
                every { waitFor() } returns 0
              }
        }

    val (exitCode, output) = DockerManager.execWithOutput("ps")

    assertThat(exitCode).isEqualTo(0)
    assertThat(output).isNull()
  }

  // --- recreateContainer ---

  @Test
  fun `recreateContainer should return true when all steps succeed`() {
    mockkConstructor(ProcessBuilder::class)
    every { anyConstructed<ProcessBuilder>().redirectErrorStream(true) } returns
        mockk(relaxed = true) {
          every { start() } returns
              mockk {
                every { inputStream } returns ByteArrayInputStream("done".toByteArray())
                every { waitFor() } returns 0
              }
        }

    assertThat(DockerManager.recreateContainer("pihole")).isTrue()
  }

  @Test
  fun `recreateContainer should return false when up step fails`() {
    mockkConstructor(ProcessBuilder::class)
    val successProcess =
        mockk<Process> {
          every { inputStream } returns ByteArrayInputStream(ByteArray(0))
          every { waitFor() } returns 0
        }
    val failProcess =
        mockk<Process> {
          every { inputStream } returns ByteArrayInputStream("error".toByteArray())
          every { waitFor() } returns 1
        }
    // stop -> success, rm -> success, up -> fail
    every { anyConstructed<ProcessBuilder>().redirectErrorStream(true) } returnsMany
        listOf(
            mockk(relaxed = true) { every { start() } returns successProcess },
            mockk(relaxed = true) { every { start() } returns successProcess },
            mockk(relaxed = true) { every { start() } returns failProcess },
        )

    assertThat(DockerManager.recreateContainer("pihole")).isFalse()
  }

  @Test
  fun `recreateContainer should return false when exception is thrown`() {
    mockkConstructor(ProcessBuilder::class)
    every { anyConstructed<ProcessBuilder>().redirectErrorStream(true) } returns
        mockk(relaxed = true) { every { start() } throws RuntimeException("docker not found") }

    assertThat(DockerManager.recreateContainer("pihole")).isFalse()
  }
}
