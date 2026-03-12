# 1. Build Stage
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Skip tests to speed up build
RUN mvn clean package -DskipTests

# 2. Run Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# SECURITY UPGRADE: Create a non-root user and group
RUN addgroup -S talantigroup && adduser -S talantiuser -G talantigroup

# Copy the built JAR from the previous stage
COPY --from=build /app/target/*.jar app.jar

# Create the uploads directory and give the non-root user ownership
RUN mkdir uploads && chown -R talantiuser:talantigroup /app

# Switch to the non-root user
USER talantiuser:talantigroup

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]