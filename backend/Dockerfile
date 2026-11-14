FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN useradd --system --uid 10001 leadnest
COPY --from=build /app/target/leadnest-crm-*.jar /app/app.jar
USER leadnest
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
