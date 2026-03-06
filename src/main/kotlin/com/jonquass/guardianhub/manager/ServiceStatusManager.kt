package com.jonquass.guardianhub.manager

import com.jonquass.guardianhub.config.Loggable
import com.jonquass.guardianhub.core.Result
import com.jonquass.guardianhub.core.api.ServiceStatusResponse
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

object ServiceStatusManager : Loggable {
  private val logger = logger()
  private val executor = Executors.newFixedThreadPool(2)
  internal val tasks = ConcurrentHashMap<String, ServiceStatusResponse>()

  fun restartServicesAsync(services: List<String>): String {
    val taskId = UUID.randomUUID().toString()

    tasks[taskId] =
        ServiceStatusResponse(
            taskId = taskId,
            status = "pending",
            message = "Restart queued",
            progress = 0,
        )

    executor.submit {
      try {
        restartServices(taskId, services)
      } catch (e: Exception) {
        logger.error("Failed to restart services: {}", e.message, e)
        tasks[taskId] =
            ServiceStatusResponse(
                taskId = taskId,
                status = "failed",
                message = "Error: ${e.message}",
                progress = 100,
            )
      }
    }

    return taskId
  }

  private fun restartServices(
      taskId: String,
      services: List<String>,
  ) {
    logger.info("Starting service restart task: {}", taskId)

    tasks[taskId] =
        ServiceStatusResponse(
            taskId = taskId,
            status = "running",
            message = "Restarting services...",
            progress = 0,
        )

    val successfulRestarts = mutableListOf<String>()
    val failedRestarts = mutableListOf<String>()
    val totalServices = services.size

    services.forEachIndexed { index, service ->
      logger.info("Restarting service {}/{}: {}", index + 1, totalServices, service)

      val progress = ((index.toFloat() / totalServices) * 100).toInt()

      tasks[taskId] =
          ServiceStatusResponse(
              taskId = taskId,
              status = "running",
              message = "Restarting $service...",
              progress = progress,
              servicesRestarted = successfulRestarts.toList(),
              servicesFailed = failedRestarts.toList(),
          )
      val result =
          DockerManager.exec(
              "/usr/bin/docker",
              "compose",
              "up",
              "-d",
              "--force-recreate",
              "--no-deps",
              service,
          )
      if (result.isSuccess) {
        successfulRestarts.add(service)
      }
    }

    // Final status
    val finalStatus = if (failedRestarts.isEmpty()) "completed" else "failed"
    val finalMessage =
        if (failedRestarts.isEmpty()) {
          "All services restarted successfully"
        } else {
          "Completed with errors: ${failedRestarts.joinToString(", ")}"
        }

    tasks[taskId] =
        ServiceStatusResponse(
            taskId = taskId,
            status = finalStatus,
            message = finalMessage,
            progress = 100,
            servicesRestarted = successfulRestarts,
            servicesFailed = failedRestarts,
        )

    logger.info("Service restart task completed: {}", taskId)
  }

  fun getTaskStatus(taskId: String): Result<ServiceStatusResponse?> = Result.Success(tasks[taskId])
}
