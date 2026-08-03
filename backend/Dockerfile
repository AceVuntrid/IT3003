# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

# Runtime stage
FROM maven:3.9-eclipse-temurin-21
WORKDIR /app
RUN useradd --system --uid 1001 appuser
COPY --from=build /app/target/asset-management-*.jar app.jar
RUN mkdir -p /app/uploads && chown appuser /app/uploads
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
