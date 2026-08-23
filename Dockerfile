# syntax=docker/dockerfile:1.7

FROM maven:3.9.11-eclipse-temurin-21-alpine AS build
WORKDIR /workspace

COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp dependency:go-offline

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -DskipTests package && \
    cp target/nitros-games-backend-*.jar /workspace/application.jar

FROM eclipse-temurin:21-jre-alpine

LABEL org.opencontainers.image.title="Nitros Games Backend" \
      org.opencontainers.image.description="Production Spring Boot API for nitrosgames64.com"

RUN addgroup -S app && \
    adduser -S -D -H -u 10001 -G app app && \
    mkdir -p /app /var/lib/nitros-games/host-images && \
    chown -R app:app /app /var/lib/nitros-games

WORKDIR /app
COPY --from=build --chown=app:app /workspace/application.jar ./application.jar

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

USER 10001:10001
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=5 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "-jar", "/app/application.jar"]
