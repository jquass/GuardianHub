package com.jonquass.guardianhub.core.config

enum class Env(
    val category: EnvCategory,
    val displayName: String,
    val sensitive: Boolean,
    val tooltip: String?,
) {
  GUARDIAN_IP(EnvCategory.NETWORK, "Guardian Hub IP Address", false, null),
  ROUTER_IP(EnvCategory.NETWORK, "Router Gateway IP Address", false, null),
  NETWORK_CIDR(EnvCategory.NETWORK, "Network CIDR Range", false, null),
  NETWORK_DOMAIN(EnvCategory.NETWORK, "Network Domain", false, null),
  TZ(
      EnvCategory.SYSTEM,
      "Time Zone",
      false,
      "Changing timezone will restart Pi-hole and Cloudflared services to apply the new time zone.",
  ),
  LOGIN_PASSWORD(
      EnvCategory.SYSTEM,
      "Config UI Login Password",
      true,
      "Requires current password, new password, and serial number from device label. Changing this password will not restart any services.",
  ),
  PIHOLE_PASSWORD(
      EnvCategory.PI_HOLE,
      "Web Interface Password",
      true,
      "Pi-hole web interface password. Changes take effect immediately without restarting the container.",
  ),
  WIREGUARD_PASSWORD_HASH(
      EnvCategory.WIRE_GUARD,
      "Password Hash",
      true,
      "WireGuard Easy web interface password. Updates password and recreates the container to apply changes.",
  ),
  NPM_ADMIN_PASSWORD(
      EnvCategory.NPM,
      "Admin Password",
      true,
      "Nginx Proxy Manager admin password. Changes take effect immediately without restarting the container.",
  ),
  NPM_ADMIN_EMAIL(
      EnvCategory.NPM,
      "Admin Email",
      false,
      "The user email in NPM must match this to allow managing the Admin Password.",
  ),
  UNKNOWN(EnvCategory.OTHER, "", false, null),
}
