FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build --chown=10001:10001 /build/target/isa-0.0.1-SNAPSHOT.jar /app/app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
