package com.jonquass.guardianhub.resource

import com.jonquass.guardianhub.config.Loggable
import com.jonquass.guardianhub.core.config.Env
import com.jonquass.guardianhub.manager.ConfigManager
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.net.InetAddress

@Path("/homepage")
class HomepageResource : Loggable {
  private val logger = logger()

  @GET
  @Path("/link")
  @Produces(MediaType.APPLICATION_JSON)
  fun getHomepageLink(): Response =
      try {
        val dnsUrl = "http://homepage.guardian.home"
        val useDns =
            try {
              // Try to resolve the DNS name
              InetAddress.getByName("homepage.guardian.home")
              true
            } catch (e: Exception) {
              logger.debug("DNS resolution failed for homepage.guardian.home: {}", e.message)
              false
            }

        val url =
            if (useDns) {
              dnsUrl
            } else {
              val ip = ConfigManager.getRawConfigValue(Env.GUARDIAN_IP) ?: "127.0.0.1"
              "http://$ip:3001"
            }

        Response.ok(
                mapOf(
                    "status" to "success",
                    "url" to url,
                    "usedDns" to useDns,
                ),
            )
            .build()
      } catch (e: Exception) {
        logger.error("Failed to get homepage link: {}", e.message, e)
        Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(
                mapOf(
                    "status" to "error",
                    "message" to "Failed to get homepage link",
                ),
            )
            .build()
      }
}
