# Multi-stage Dockerfile for LostLink Full-Stack Application
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copy Maven wrapper and POM
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code including pre-built static frontend assets
COPY src src

# Package the application JAR
RUN ./mvnw clean package -DskipTests -B

# Production runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Expose dynamic cloud port
EXPOSE 8081

# Copy the built fat JAR
COPY --from=build /app/target/lost-link-backend-0.0.1-SNAPSHOT.jar app.jar

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

