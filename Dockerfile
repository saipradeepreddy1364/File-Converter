# Build the Spring Boot application from repo root
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY backend/pom.xml ./pom.xml
COPY backend/src ./src

RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/target/file-converter-backend-1.0.0.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
