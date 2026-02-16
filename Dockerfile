# 1. Build Stage (Optional, but good for cleanliness)
# You can skip this and just use the JAR if you build with Maven locally first
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Skip tests to speed up build, assuming you ran them locally
RUN mvn clean package -DskipTests

# 2. Run Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Copy the built JAR from the previous stage
COPY --from=build /app/target/*.jar app.jar

# Create the uploads directory so the app can write to it
RUN mkdir uploads

# Expose the port
EXPOSE 8080

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]