package com.jonquass.guardianhub.manager

import com.jonquass.guardianhub.config.Loggable
import com.jonquass.guardianhub.core.Result

object DockerManager : Loggable {
  private val logger = logger()

  internal val DEFAULT_PROCESS_BUILDER_FACTORY: (List<String>) -> Process = { args ->
    ProcessBuilder(args).redirectErrorStream(true).start()
  }

  internal var processBuilderFactory = DEFAULT_PROCESS_BUILDER_FACTORY

  fun exec(vararg args: String): Result<Unit> =
      try {
        val process = ProcessBuilder("/usr/bin/docker", *args).redirectErrorStream(true).start()
        val code = process.waitFor()
        if (code != 0) {
          return Result.error("Non-zero exit code $code")
        }
        Result.success()
      } catch (e: Exception) {
        logger.error("Exception while executing args {}: {}", args, e.message, e)
        Result.error("Exception while executing args")
      }

  fun execWithOutput(vararg args: String): Result<String> =
      try {
        val process = processBuilderFactory(listOf("/usr/bin/docker", *args))
        val output =
            process.inputStream.bufferedReader().readLine()?.trim()
                ?: return Result.error("No output while executing args")
        process.waitFor()
        Result.success(output)
      } catch (e: Exception) {
        logger.error("Exception while executing args {}: {}", args, e.message, e)
        Result.error("Exception while executing args")
      }

  fun recreateContainer(serviceName: String): Result<Unit> =
      try {
        logger.info("Recreating service {} to reload environment...", serviceName)
        ProcessBuilder(
                "/usr/bin/docker",
                "compose",
                "-f",
                "/opt/pi-stack/docker-compose.yml",
                "stop",
                serviceName)
            .redirectErrorStream(true)
            .start()
            .waitFor()
        ProcessBuilder(
                "/usr/bin/docker",
                "compose",
                "-f",
                "/opt/pi-stack/docker-compose.yml",
                "rm",
                "-f",
                serviceName)
            .redirectErrorStream(true)
            .start()
            .waitFor()
        val upProcess =
            ProcessBuilder(
                    "/usr/bin/docker",
                    "compose",
                    "-f",
                    "/opt/pi-stack/docker-compose.yml",
                    "up",
                    "-d",
                    serviceName)
                .redirectErrorStream(true)
                .start()
        val output = upProcess.inputStream.bufferedReader().use { it.readText() }
        val exitCode = upProcess.waitFor()
        logger.info("Service recreate exit code: {}, output: {}", exitCode, output)
        if (exitCode == 0) {
          return Result.success()
        }
        return Result.error()
      } catch (e: Exception) {
        logger.error("Failed to restart {}: {}", serviceName, e.message, e)
        return Result.error()
      }
}
