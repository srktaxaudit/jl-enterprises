# ── Stage 1: build ──────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Cache dependencies first
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
# Then build
COPY src ./src
RUN mvn -q -B -DskipTests package

# ── Stage 2: run ────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre AS run
WORKDIR /app
# Run as a non-root user
RUN useradd -r -u 1001 appuser
COPY --from=build /app/target/*.jar app.jar
USER appuser
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
