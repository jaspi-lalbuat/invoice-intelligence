FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace
# Copy project files
COPY . .

# Ensure the Gradle wrapper is executable and use it to build with the project's configured Gradle version
RUN chmod +x ./gradlew || true
RUN ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
EXPOSE 8080

# Copy fat jar produced by Spring Boot
COPY --from=builder /workspace/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
