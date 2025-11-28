# Build Stage
FROM maven:3.9-eclipse-temurin-17 AS build

# Set the working directory
WORKDIR /app

# Copy the pom.xml and source code
COPY pom.xml .
COPY src ./src

# Package the application
RUN mvn clean package -DskipTests

# Runtime Stage
FROM eclipse-temurin:17-jre

# Set the working directory
WORKDIR /app

# Create a non-root user
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

# Copy the JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the application port
EXPOSE 8081

# Switch to non-root user
USER appuser

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]