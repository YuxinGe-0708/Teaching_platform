ARG JAVA_IMAGE=eclipse-temurin:8-jre-jammy
FROM ${JAVA_IMAGE}
WORKDIR /app
COPY target/user-service.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
