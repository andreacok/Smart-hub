# syntax=docker/dockerfile:1

# ---- build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- runtime ----
# Immagine multi-architettura (amd64 / arm64): gira su mini PC, NAS e Raspberry Pi a 64 bit.
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 smarthub && mkdir -p /app/data && chown smarthub /app/data
COPY --from=build /app/target/smart-hub-backend.jar app.jar
USER smarthub
ENV SMARTTHINGS_OAUTH_TOKEN_STORE=/app/data/smartthings-oauth.json \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
