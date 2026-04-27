# Build stage
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app
# Copy maven wrapper and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
# Give execution rights to the maven wrapper
RUN sed -i 's/\r$//' mvnw && chmod +x ./mvnw
# Download dependencies
RUN ./mvnw dependency:go-offline -B
# Copy source code
COPY src src
# Build the application
RUN ./mvnw package -DskipTests

# Run stage
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
# Copy the built jar
COPY --from=builder /app/target/*.jar app.jar
# Expose port
EXPOSE 8080
# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
