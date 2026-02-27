package com.jonquass.guardianhub.manager

import com.jonquass.guardianhub.config.Loggable

object DockerManager : Loggable {
    private val logger = logger()

    fun exec(vararg args: String): Boolean =
        try {
            val process =
                ProcessBuilder("/usr/bin/docker", *args)
                    .redirectErrorStream(true)
                    .start()

            process.waitFor() == 0
        } catch (e: Exception) {
            logger.error("Exception while executing args {}: {}", args, e.message, e)
            false
        }

    fun execWithOutput(vararg args: String): Pair<Int, String?> =
        try {
            val process =
                ProcessBuilder("/usr/bin/docker", *args)
                    .redirectErrorStream(true)
                    .start()
            val output =
                process.inputStream
                    .bufferedReader()
                    .readLine()
                    ?.trim()
            val exitCode = process.waitFor()
            Pair(exitCode, output)
        } catch (e: Exception) {
            logger.error("Exception while executing args {}: {}", args, e.message, e)
            Pair(-1, null)
        }

    fun recreateContainer(serviceName: String): Boolean =
        try {
            logger.info("Recreating service {} to reload environment...", serviceName)
            ProcessBuilder("/usr/bin/docker", "compose", "-f", "/opt/pi-stack/docker-compose.yml", "stop", serviceName)
                .redirectErrorStream(true)
                .start()
                .waitFor()
            ProcessBuilder("/usr/bin/docker", "compose", "-f", "/opt/pi-stack/docker-compose.yml", "rm", "-f", serviceName)
                .redirectErrorStream(true)
                .start()
                .waitFor()
            val upProcess =
                ProcessBuilder("/usr/bin/docker", "compose", "-f", "/opt/pi-stack/docker-compose.yml", "up", "-d", serviceName)
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
