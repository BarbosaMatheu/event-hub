# ---------- build stage ----------
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Resolve dependencies first so this layer is cached until pom.xml changes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true

COPY src ./src
RUN mvn -B -q -DskipTests package

# ---------- runtime stage ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Never run as root inside the container.
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/target/*.jar app.jar
USER app

EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=45s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# MaxRAMPercentage makes the JVM size its heap from the container's memory limit.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
