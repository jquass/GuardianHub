package com.jonquass.guardianhub.manager

import com.jonquass.guardianhub.config.Loggable
import com.jonquass.guardianhub.core.Result
import com.jonquass.guardianhub.core.api.HomepageLinkResponse
import com.jonquass.guardianhub.core.config.Env
import java.net.InetAddress
import java.net.UnknownHostException

object HomepageManager : Loggable {
  private val logger = logger()

  fun getHomepageLink(): Result<HomepageLinkResponse> {
    val dnsUrl = "http://homepage.guardian.home"
    val useDns =
        try {
          // Try to resolve the DNS name
          InetAddress.getByName("homepage.guardian.home")
          true
        } catch (e: UnknownHostException) {
          logger.info("DNS resolution failed for homepage.guardian.home: {}", e.message)
          false
        }
    val url =
        if (useDns) {
          dnsUrl
        } else {
          val ip = ConfigManager.getRawConfigValue(Env.GUARDIAN_IP) ?: "127.0.0.1"
          "http://$ip:3001"
        }
    return Result.Success(HomepageLinkResponse(url, useDns))
  }
}
