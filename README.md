[![codecov](https://codecov.io/gh/jquass/GuardianHub/branch/main/graph/badge.svg)](https://codecov.io/gh/jquass/GuardianHub)

# Guardian Hub Config UI

A web interface for managing the [Guardian Hub Stack](https://github.com/jquass/guardian-hub-stack) — a Raspberry Pi-based network appliance providing network-wide ad blocking, VPN access, and DNS privacy.

## Overview

Rather than juggling multiple service dashboards, Guardian Hub Config UI provides a single interface to configure and manage all Guardian Hub services. It is designed to run as part of the [guardian-hub-stack](https://github.com/jquass/guardian-hub-stack) deployment and is not intended to be run standalone.

## Tech Stack

- **Backend:** Kotlin, Jersey (JAX-RS), Grizzly HTTP Server
- **Frontend:** Vanilla JavaScript, CSS3
- **Build:** Maven
- **Runtime:** Eclipse Temurin JDK 21, Alpine Linux
- **Deployment:** Docker multi-arch (ARM64 optimized for Raspberry Pi)

## Building

```bash
# Run tests
mvn verify

# Build JAR
mvn clean package

# Build Docker image for Raspberry Pi (ARM64)
podman build --platform linux/arm64 -t guardian-hub-config:latest .

# Build Docker image for local testing (x86_64)
podman build --platform linux/amd64 -t guardian-hub-config:latest .
```

## Deployment

This application is deployed as part of the Guardian Hub Stack. See [guardian-hub-stack](https://github.com/jquass/guardian-hub-stack) for full deployment instructions.

## License

GNU General Public License v3.0 — see [LICENSE](LICENSE) for details.

## Author

Jon Quass — [@jquass](https://github.com/jquass)