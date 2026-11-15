# syntax=docker/dockerfile:1

# Stage 1: Build using Java 17
FROM maven:3-eclipse-temurin-17-noble AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp -DskipTests package

# Stage 2: Run using Java 17
FROM eclipse-temurin:17-jre-noble
WORKDIR /app

# Create an unprivileged application user
RUN groupadd --gid 10001 appgroup \
    && useradd --uid 10001 --gid appgroup --no-create-home \
       --shell /usr/sbin/nologin appuser \
    && mkdir -p /app/certs

# Certificate bundle for verified RDS TLS connections
ADD https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem /app/certs/global-bundle.pem
RUN chmod 0444 /app/certs/global-bundle.pem

COPY --from=build --chown=10001:10001 /build/target/*.jar /app/app.jar

USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=65.0", "-jar", "/app/app.jar"]