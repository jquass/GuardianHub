package com.jonquass.guardianhub.manager

import com.jonquass.guardianhub.config.Loggable
import com.jonquass.guardianhub.core.Result

object DockerManager : Loggable {
  private val logger = logger()

  fun exec(vararg args: String): Result<Nothing?> =
      try {
        val process = ProcessBuilder("/usr/bin/docker", *args).redirectErrorStream(true).start()
        val code = process.waitFor()
        if (code != 0) {
          return Result.Error("Non-zero exit code $code")
        }
        Result.Success(null)
      } catch (e: Exception) {
        logger.error("Exception while executing args {}: {}", args, e.message, e)
        Result.Error("Exception while executing args")
      }

  fun execWithOutput(vararg args: String): Result<String> =
      try {
        val process = ProcessBuilder("/usr/bin/docker", *args).redirectErrorStream(true).start()
        val output =
            process.inputStream.bufferedReader().readLine()?.trim()
                ?: return Result.Error("No output while executing args")
        process.waitFor()
        Result.Success(output)
      } catch (e: Exception) {
        logger.error("Exception while executing args {}: {}", args, e.message, e)
        Result.Error("Exception while executing args")
      }

  fun recreateContainer(serviceName: String): Boolean =
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
        exitCode == 0
      } catch (e: Exception) {
        logger.error("Failed to restart {}: {}", serviceName, e.message, e)
        false
      }
}
