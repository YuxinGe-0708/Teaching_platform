ARG MAVEN_IMAGE=maven:3.8.8-eclipse-temurin-8
ARG JAVA_IMAGE=eclipse-temurin:8-jre-jammy
FROM ${MAVEN_IMAGE} AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline || true
COPY src ./src
RUN mvn -q -DskipTests package

FROM ${JAVA_IMAGE}
WORKDIR /app
COPY --from=build /app/target/assessment-service.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
