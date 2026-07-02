# Build stage using Eclipse Temurin JDK 17 to match pom.xml
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src ./src

RUN chmod +x mvnw && ./mvnw clean package -DskipTests

# Runtime stage using Eclipse Temurin JRE 17
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /workspace/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT} -jar app.jar"]
