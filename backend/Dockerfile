# --- build stage ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# --- runtime stage ---
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN useradd --no-create-home --shell /usr/sbin/nologin appuser
COPY --from=build /build/target/*.jar app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "app.jar"]
