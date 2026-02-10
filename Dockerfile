# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build JAR
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Install required tools
RUN apk add --no-cache \
    docker-cli \
    docker-cli-compose \
    util-linux \
    curl \
    apache2-utils

# Copy JAR from build stage
COPY --from=build /app/target/guardian-hub-config.jar ./app.jar

# Copy static resources from build stage
COPY --from=build /app/src/main/resources/static ./static

# Expose port
EXPOSE 8888

# Set environment to serve static files from current directory
ENV STATIC_PATH=/app/static

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]