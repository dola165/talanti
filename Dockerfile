# Only use the Run Stage
FROM amazoncorretto:21-alpine-jdk
WORKDIR /app

# Security: non-root user
RUN addgroup -S talantigroup && adduser -S talantiuser -G talantigroup
RUN mkdir uploads && chown -R talantiuser:talantigroup /app
USER talantiuser:talantigroup

# Copy the JAR you built locally from your target folder
COPY target/talanti-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]