package com.jonquass.guardianhub.managers

import com.jonquass.guardianhub.config.Loggable
import com.jonquass.guardianhub.core.api.ConfigResponse
import com.jonquass.guardianhub.core.config.CategoryInfo
import com.jonquass.guardianhub.core.config.ConfigEntry
import com.jonquass.guardianhub.core.config.Env
import com.jonquass.guardianhub.core.config.EnvCategory
import jakarta.ws.rs.core.Response
import java.io.File

object ConfigManager : Loggable {
    private val logger = logger()

    private val configFile = File("/opt/pi-stack/.env")

    /**
     * Used for the UI, masks sensitive fields
     */
    fun readConfig(): Response {
        return try {
            val entries = mutableListOf<ConfigEntry>()

            configFile.readLines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    return@forEach
                }

                val parts = trimmed.split("=", limit = 2)
                if (parts.size == 2) {
                    val keyName = parts[0].trim()
                    var value = parts[1].trim()

                    // Remove surrounding quotes if present
                    if ((value.startsWith("'") && value.endsWith("'")) ||
                        (value.startsWith("\"") && value.endsWith("\""))
                    ) {
                        value = value.substring(1, value.length - 1)
                    }

                    // Find matching enum
                    val env = Env.entries.find { it.name == keyName } ?: Env.UNKNOWN

                    // Skip UNKNOWN entries
                    if (env == Env.UNKNOWN) {
                        return@forEach
                    }

                    // Mask sensitive values
                    val displayValue = if (env.sensitive) "••••••••" else value

                    entries.add(
                        ConfigEntry(
                            key = env.name,
                            value = displayValue,
                            categoryName = env.category.displayName,
                            description = env.displayName,
                            sensitive = env.sensitive,
                            tooltip = env.tooltip,
                        ),
                    )
                }
            }

            // Get unique categories from entries (only categories that have entries)
            val categoriesWithEntries =
                entries
                    .map { it.categoryName }
                    .distinct()
                    .mapNotNull { categoryName ->
                        EnvCategory.entries.find { it.displayName == categoryName }
                    }.map { category ->
                        CategoryInfo(
                            name = category.displayName,
                            tooltip = category.tooltip,
                        )
                    }

            val configResponse =
                ConfigResponse(
                    categories = categoriesWithEntries,
                    entries = entries,
                )

            Response
                .ok(
                    mapOf(
                        "status" to "success",
                        "config" to configResponse,
                    ),
                ).build()
        } catch (e: Exception) {
            logger.error("Failed to read config: {}", e.message, e)
            Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(
                    mapOf(
                        "status" to "error",
                        "message" to "Failed to read config: ${e.message}",
                    ),
                ).build()
        }
    }

    /**
     * Upsert a configuration value in the .env file
     */
    fun upsertConfig(
        key: Env,
        value: String,
    ) {
        validateConfig()
        logger.trace("Updating .env file for key {}", key)
        val lines = configFile.readLines().toMutableList()
        var updated = false
        val escapedValue =
            if (key.sensitive) {
                "'$value'"
            } else {
                value
            }
        for (i in lines.indices) {
            val line = lines[i].trim()
            if (line.startsWith("$key=")) {
                lines[i] = "$key=$escapedValue"
                updated = true
                break
            }
        }
        if (!updated) {
            lines.add("$key=$escapedValue")
        }
        configFile.writeText(lines.joinToString("\n"))
        logger.info("Upserted .env file for key {}", key)
    }

    /**
     * Read a raw config value without masking (for internal use)
     */
    fun getRawConfigValue(key: Env): String? {
        validateConfig()
        configFile.readLines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                return@forEach
            }

            val parts = trimmed.split("=", limit = 2)
            if (parts.size == 2 && parts[0].trim() == key.name) {
                var value = parts[1].trim()

                // Remove surrounding quotes if present (handles bcrypt hashes)
                if ((value.startsWith("'") && value.endsWith("'")) ||
                    (value.startsWith("\"") && value.endsWith("\""))
                ) {
                    value = value.substring(1, value.length - 1)
                }

                return value
            }
        }

        return null
    }

    private fun validateConfig() {
        if (!configFile.exists()) {
            throw Exception("Configuration file not found: ${configFile.absolutePath}")
        }
    }
}
