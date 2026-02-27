package com.jonquass.guardianhub.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface Loggable {
  fun logger(): Logger = LoggerFactory.getLogger(this::class.java)
}
