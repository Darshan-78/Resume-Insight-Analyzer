# --- Build Stage ---
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy the pom.xml to download dependencies (helps with Docker layer caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the source code and build the package (skipping tests for faster deployment)
COPY src ./src
RUN mvn clean package -DskipTests

# --- Run Stage ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the packaged jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose port 8080
EXPOSE 8080

# Environment variables will be passed in at runtime by Render.
# Spring Boot automatically maps env variables to application properties.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70.0", "-XX:+UseSerialGC", "-Xss256k", "-jar", "app.jar"]

