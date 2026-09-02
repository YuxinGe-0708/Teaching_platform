FROM eclipse-temurin:8-jre-jammy
WORKDIR /app
COPY learning-service.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]