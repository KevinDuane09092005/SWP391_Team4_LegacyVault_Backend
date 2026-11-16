# Stage 1: Build JAR with Maven & Java 21
FROM maven:3.9.9-eclipse-temurin-21-jammy AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

# Stage 2: Lightweight Java 21 JRE runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN groupadd --system appuser && useradd --system --gid appuser appuser

COPY --from=build /app/target/*.jar /app/app.jar

RUN mkdir -p /tmp/legacyvault-uploads && chown -R appuser:appuser /app /tmp/legacyvault-uploads
USER appuser

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
