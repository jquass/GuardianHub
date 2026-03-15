package com.jonquass.guardianhub.manager

import com.jonquass.guardianhub.core.getOrThrow
import com.jonquass.guardianhub.manager.DockerManager.DEFAULT_PROCESS_BUILDER_FACTORY
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
    DockerManager.processBuilderFactory = DEFAULT_PROCESS_BUILDER_FACTORY
  }

  // --- exec ---

  @Test
  fun `exec should return success when process exits with code 0`() {
    mockkConstructor(ProcessBuilder::class)
    every { anyConstructed<ProcessBuilder>().redirectErrorStream(true) } returns
        mockk(relaxed = true) { every { start() } returns mockk { every { waitFor() } returns 0 } }

    assertThat(DockerManager.exec("ps").isSuccess).isTrue
  }

  @Test
  fun `exec should return error when process exits with non-zero code`() {
    mockkConstructor(ProcessBuilder::class)
    every { anyConstructed<ProcessBuilder>().redirectErrorStream(true) } returns
        mockk(relaxed = true) { every { start() } returns mockk { every { waitFor() } returns 1 } }

    assertThat(DockerManager.exec("ps").isError).isTrue
  }

  @Test
  fun `exec should return error when process throws exception`() {
    mockkConstructor(ProcessBuilder::class)
    every { anyConstructed<ProcessBuilder>().redirectErrorStream(true) } returns
        mockk(relaxed = true) { every { start() } throws RuntimeException("docker not found") }

    assertThat(DockerManager.exec("ps").isError).isTrue
  }

  // --- execWithOutput ---

  @Test
  fun `execWithOutput should return exit code and output on success`() {
    DockerManager.processBuilderFactory = {
      mockk {
        every { inputStream } returns ByteArrayInputStream("some output".toByteArray())
        every { waitFor() } returns 0
      }
    }

    val result = DockerManager.execWithOutput("ps")
    assertThat(result.isSuccess).isTrue
    assertThat(result.getOrThrow()).isEqualTo("some output")
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

    val result = DockerManager.execWithOutput("ps")

    assertThat(result.isSuccess).isTrue
    assertThat(result.getOrThrow()).isEqualTo("error output")
  }

  @Test
  fun `execWithOutput should return error when exception is thrown`() {
    DockerManager.processBuilderFactory = { throw RuntimeException("docker not found") }

    val result = DockerManager.execWithOutput("ps")
    assertThat(result.isError).isTrue
  }

  @Test
  fun `execWithOutput should return null output when process produces no output`() {
    DockerManager.processBuilderFactory = {
      mockk {
        every { inputStream } returns ByteArrayInputStream(ByteArray(0))
        every { waitFor() } returns 0
      }
    }

    val result = DockerManager.execWithOutput("ps")
    assertThat(result.isError).isTrue
  }

  @Test
  fun `execWithOutput should return success with output when process exits with non-zero code`() {
    // The current impl calls waitFor() but doesn't check exit code - it returns success regardless
    // This covers the branch where readLine() returns a value AND exitCode != 0
    DockerManager.processBuilderFactory = {
      mockk {
        every { inputStream } returns ByteArrayInputStream("error output".toByteArray())
        every { waitFor() } returns 1
      }
    }

    val result = DockerManager.execWithOutput("ps")
    assertThat(result.isSuccess).isTrue
    assertThat(result.getOrThrow()).isEqualTo("error output")
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

    assertThat(DockerManager.recreateContainer("pihole").isSuccess).isTrue
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

    assertThat(DockerManager.recreateContainer("pihole").isError).isTrue
  }

  @Test
  fun `recreateContainer should return false when exception is thrown`() {
    mockkConstructor(ProcessBuilder::class)
    every { anyConstructed<ProcessBuilder>().redirectErrorStream(true) } returns
        mockk(relaxed = true) { every { start() } throws RuntimeException("docker not found") }

    assertThat(DockerManager.recreateContainer("pihole").isError).isTrue
  }

  @Test
  fun `recreateContainer should return error when process produces no output`() {
    DockerManager.processBuilderFactory = {
      mockk {
        every { inputStream } returns ByteArrayInputStream(ByteArray(0))
        every { waitFor() } returns 0
      }
    }

    assertThat(DockerManager.recreateContainer("pihole").isError).isTrue
  }

  @Test
  fun `recreateContainer should return error when up process exits with non-zero code and has output`() {
    val successProcess = mockk<Process> { every { waitFor() } returns 0 }
    val failProcess =
        mockk<Process> {
          every { inputStream } returns
              ByteArrayInputStream("container failed to start".toByteArray())
          every { waitFor() } returns 1
        }

    // stop -> success, rm -> success, up -> non-zero exit with output
    every { anyConstructed<ProcessBuilder>().redirectErrorStream(true) } returnsMany
        listOf(
            mockk(relaxed = true) { every { start() } returns successProcess },
            mockk(relaxed = true) { every { start() } returns successProcess },
            mockk(relaxed = true) { every { start() } returns failProcess },
        )

    assertThat(DockerManager.recreateContainer("pihole").isError).isTrue
  }

  @Test
  fun `recreateContainer should return success when up process exits with code 0 and has output`() {
    val successProcess = mockk<Process> { every { waitFor() } returns 0 }
    val upSuccessProcess =
        mockk<Process> {
          every { inputStream } returns
              ByteArrayInputStream("Started container pihole".toByteArray())
          every { waitFor() } returns 0
        }

    every { anyConstructed<ProcessBuilder>().redirectErrorStream(true) } returnsMany
        listOf(
            mockk(relaxed = true) { every { start() } returns successProcess },
            mockk(relaxed = true) { every { start() } returns successProcess },
            mockk(relaxed = true) { every { start() } returns upSuccessProcess },
        )

    assertThat(DockerManager.recreateContainer("pihole").isSuccess).isTrue
  }
}
