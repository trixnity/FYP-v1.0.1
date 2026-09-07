# syntax=docker/dockerfile:1

# ---- Build stage: Temurin JDK 17 (matches pom.xml <java.version>17</java.version>) ----
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src ./src

RUN chmod +x mvnw && ./mvnw -B clean package -DskipTests

# ---- Runtime stage: Temurin JRE 17 + Stockfish ----
FROM eclipse-temurin:17-jre

# Stockfish is in the Ubuntu 'universe' repo; installs to /usr/games/stockfish
RUN apt-get update && \
    apt-get install -y --no-install-recommends stockfish && \
    rm -rf /var/lib/apt/lists/* && \
    test -x /usr/games/stockfish

ENV STOCKFISH_PATH=/usr/games/stockfish \
    SPRING_PROFILES_ACTIVE=prod

WORKDIR /app
COPY --from=builder /workspace/target/*.jar app.jar

EXPOSE 8080

# The host injects PORT. MaxRAMPercentage + SerialGC keep the JVM inside a small
# (~512 MB) free instance.
ENTRYPOINT ["sh", "-c", "java -XX:MaxRAMPercentage=70.0 -XX:+UseSerialGC -Dserver.port=${PORT:-8080} -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod} -jar app.jar"]
