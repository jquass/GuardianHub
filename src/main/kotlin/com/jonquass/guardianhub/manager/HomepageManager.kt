package com.jonquass.guardianhub.manager

import com.jonquass.guardianhub.config.Loggable
import com.jonquass.guardianhub.core.Result
import com.jonquass.guardianhub.core.api.HomepageLinkResponse
import com.jonquass.guardianhub.core.config.Env
import java.net.InetAddress
import java.net.UnknownHostException

object HomepageManager : Loggable {
  private val logger = logger()

  var dnsResolver: (String) -> Boolean = { hostname ->
    try {
      InetAddress.getByName(hostname)
      true
    } catch (e: UnknownHostException) {
      logger.info("DNS resolution failed for {}: {}", hostname, e.message)
      false
    }
  }

  fun getHomepageLink(): Result<HomepageLinkResponse> {
    val dnsUrl = "http://homepage.guardian.home"
    val useDns = dnsResolver("homepage.guardian.home")
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
