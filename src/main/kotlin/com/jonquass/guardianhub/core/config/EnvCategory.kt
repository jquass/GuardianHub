package com.jonquass.guardianhub.core.config

enum class EnvCategory(
    val displayName: String,
    val tooltip: String?,
) {
  NETWORK(
      "Network",
      "Network settings are auto-detected on boot. If something looks wrong, restart the Guardian Hub to refresh network configuration.",
  ),
  SYSTEM("System", null),
  PI_HOLE("Pi-hole", null),
  WIRE_GUARD("Wire Guard", null),
  NPM("NPM", null),
  OTHER("Other", null),
}
