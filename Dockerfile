# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn clean package -DskipTests -q

# ─── Stage 2: Run ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar


EXPOSE 8080

EXPOSE 8761

EXPOSE 8081
# Directory for local file uploads (when S3 is not configured)
RUN mkdir -p /app/uploads

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8086
EXPOSE 8083
EXPOSE 8085
EXPOSE 8084
EXPOSE 8082
EXPOSE 8087

ENTRYPOINT ["java", "-jar", "app.jar"]
