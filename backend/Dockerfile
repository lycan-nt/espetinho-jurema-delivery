# Build stage — requires Docker BuildKit network for Maven Central
FROM maven:3.9.9-eclipse-temurin-21-alpine AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /src/target/*.jar /app/app.jar
EXPOSE 9090
VOLUME ["/app/data"]
ENV H2_FILE_PATH=/app/data/espetinho
ENV H2_BACKUP_DIR=/app/data/backups
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
