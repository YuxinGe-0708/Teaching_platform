FROM eclipse-temurin:8-jre-jammy
WORKDIR /app
COPY target/learning-service.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
