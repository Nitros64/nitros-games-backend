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

FROM eclipse-temurin:21-jre-alpine@sha256:974b08960c5d96694c780e65b2d5705268ab1e1ca1a0dd0caf4ba6c3fe34d699

ARG RDS_CA_BUNDLE_URL=https://truststore.pki.rds.amazonaws.com/eu-west-1/eu-west-1-bundle.pem
ARG RDS_CA_BUNDLE_SHA256=a11cf9a1d0aadd7db86f92cbaa496466daeb501bf1c5e429d8ce8914a01c15d6
ARG RDS_CA_SHA1=60:63:E7:C0:47:2F:51:95:26:11:0D:BF:86:A2:73:B5:C5:19:53:F3

LABEL org.opencontainers.image.title="Nitros Games Backend" \
      org.opencontainers.image.description="Production Spring Boot API for nitrosgames64.com"

RUN wget --quiet "${RDS_CA_BUNDLE_URL}" -O /tmp/rds-ca-bundle.pem && \
    echo "${RDS_CA_BUNDLE_SHA256}  /tmp/rds-ca-bundle.pem" | sha256sum --check --strict && \
    awk '/-----BEGIN CERTIFICATE-----/ { copy = 1 } copy { print } /-----END CERTIFICATE-----/ { exit }' \
        /tmp/rds-ca-bundle.pem > /tmp/rds-ca-rsa2048-g1.pem && \
    test "$(keytool -printcert -file /tmp/rds-ca-rsa2048-g1.pem | \
        awk -F': ' '/SHA1:/{print $2}')" = "${RDS_CA_SHA1}" && \
    keytool -importcert -noprompt -trustcacerts \
        -alias amazon-rds-eu-west-1-root-ca-rsa2048-g1 \
        -file /tmp/rds-ca-rsa2048-g1.pem \
        -cacerts -storepass changeit && \
    rm -f /tmp/rds-ca-bundle.pem /tmp/rds-ca-rsa2048-g1.pem && \
    addgroup -S app && \
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
