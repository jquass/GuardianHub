[![codecov](https://codecov.io/gh/jquass/GuardianHub/branch/main/graph/badge.svg)](https://codecov.io/gh/jquass/GuardianHub)


# Guardian Hub Config UI

A unified web interface for managing the Guardian Hub - a Raspberry Pi-based network appliance providing network-wide ad blocking, VPN access, and DNS privacy.

## Overview

Guardian Hub Config UI provides a single, intuitive interface to configure and manage all aspects of your Guardian Hub device. Instead of accessing multiple web interfaces (Pi-hole, WireGuard, etc.), manage everything from one clean dashboard.

This application is designed to run exclusively on the Guardian Hub Raspberry Pi appliance and requires the [guardian-hub-stack](https://github.com/jquass/guardian-hub-stack) deployment.

## Features

- 🛡️ **Unified Management** - Single interface for all Guardian Hub services
- 🔐 **Password Management** - Update Pi-hole and WireGuard credentials without SSH access
- 🌐 **Network Configuration** - View auto-detected network settings
- ⏰ **Timezone Control** - Change system timezone with automatic service coordination
- 📱 **Responsive Design** - Works on desktop, tablet, and mobile devices
- 🎨 **Clean Interface** - Purpose-built for the Guardian Hub appliance
- ⚡ **Real-time Updates** - Changes take effect immediately with automatic service restarts

## Tech Stack

### Hardware
- **Raspberry Pi 5** (recommended) - 4GB or 8GB RAM
- **Raspberry Pi 4** (supported) - 4GB or 8GB RAM
- MicroSD card (32GB minimum, 64GB recommended)
- Stable power supply (official Raspberry Pi PSU recommended)

### Software
- **Backend:** Kotlin 1.9.22, Jersey 3.1.5 (JAX-RS), Grizzly HTTP Server 4.0.2
- **Frontend:** Vanilla JavaScript, CSS3
- **Build:** Maven 3.9
- **Runtime:** Eclipse Temurin JDK 21, Alpine Linux
- **Deployment:** Docker multi-arch (ARM64 optimized)

## Prerequisites

This application requires:
- Guardian Hub Stack deployed on Raspberry Pi ([guardian-hub-stack](https://github.com/jquass/guardian-hub-stack))
- Docker and Docker Compose
- Network access to the Guardian Hub device

## Building

### Build JAR
```bash
mvn clean package
```

### Build Docker Image
```bash
# For Raspberry Pi (ARM64)
podman build --platform linux/arm64 -t guardian-hub-config:latest .

# For testing on x86_64
podman build --platform linux/amd64 -t guardian-hub-config:latest .
```

### Multi-arch Build and Deploy
```bash
# Build for ARM64
podman build --platform linux/arm64 -t guardian-hub-config:latest .

# Save and transfer to Raspberry Pi
podman save -o guardian-hub-config.tar guardian-hub-config:latest
scp guardian-hub-config.tar admin@guardian-hub.local:/tmp/

# On Raspberry Pi, load and deploy
ssh admin@guardian-hub.local
docker load -i /tmp/guardian-hub-config.tar
cd /opt/pi-stack
docker compose up -d config-ui
```

## Usage

Once deployed as part of the Guardian Hub Stack, access the Config UI at:

- **Via device IP**
- **Via custom domain:** http://config.guardian.home

The interface provides:
- Network settings overview (auto-detected on boot)
- Password management for Pi-hole and WireGuard web interfaces
- System timezone configuration
- Tooltips and help text for all settings

## API Endpoints

- `GET /api/config` - Retrieve all configuration settings
- `GET /api/health` - Health check endpoint
- `POST /api/password/pihole` - Update Pi-hole web interface password
- `POST /api/password/wireguard` - Update WireGuard web interface password (recreates container)
- `GET /api/timezone` - List available timezones
- `POST /api/timezone` - Update system timezone (restarts services)

## License

GNU General Public License v3.0 - See [LICENSE](LICENSE) file for details.

## Author

Jon Quass - [@jquass](https://github.com/jquass)

## Related Projects

- [guardian-hub-stack](https://github.com/jquass/guardian-hub-stack) - Complete Guardian Hub deployment configuration (required)