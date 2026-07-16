# Build stage - Maven + JDK 21 (the repo carries no Maven wrapper; the image provides mvn)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml ./
# Warm the dependency layer so source-only rebuilds don't re-download the world
RUN mvn -q -B dependency:go-offline
COPY src ./src
# Tests run in CI and the dev loop; the image build only packages
RUN mvn -q -B -DskipTests package

# Runtime stage - JRE only, non-root
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S iams && adduser -S iams -G iams
USER iams
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
