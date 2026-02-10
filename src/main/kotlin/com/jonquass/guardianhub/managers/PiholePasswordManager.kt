package com.jonquass.guardianhub.managers

import com.jonquass.guardianhub.config.Loggable

object PiholePasswordManager : Loggable {
    private val logger = logger()

    fun setPiholePassword(password: String): Boolean =
        try {
            logger.info("Setting Pi-hole password via docker exec...")

            // Run: docker exec pihole pihole setpassword 'password'
            val process =
                ProcessBuilder(
                    "/usr/bin/docker",
                    "exec",
                    "pihole",
                    "pihole",
                    "setpassword",
                    password,
                ).redirectErrorStream(true)
                    .start()

            val output = process.inputStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            logger.info("Pi-hole setpassword exit code: {}", exitCode)
            logger.info("Pi-hole setpassword output: {}", output)

            exitCode == 0
        } catch (e: Exception) {
            logger.error("Exception setting Pi-hole password: {}", e.message, e)
            false
        }
}
