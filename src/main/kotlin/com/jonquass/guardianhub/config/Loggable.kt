package com.jonquass.guardianhub.config

import com.jonquass.guardianhub.core.ExcludeManagerCheck
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface Loggable {

  @ExcludeManagerCheck fun logger(): Logger = LoggerFactory.getLogger(this::class.java)
}
