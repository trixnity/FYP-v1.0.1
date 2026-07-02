# Build stage using Eclipse Temurin JDK 17 to match pom.xml
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src ./src

RUN chmod +x mvnw && ./mvnw clean package -DskipTests

# Runtime stage using Eclipse Temurin JRE 17
FROM eclipse-temurin:17-jre

RUN apt-get update && \
    apt-get install -y stockfish && \
    rm -rf /var/lib/apt/lists/*

ENV STOCKFISH_PATH=/usr/games/stockfish

WORKDIR /app

COPY --from=builder /workspace/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]
