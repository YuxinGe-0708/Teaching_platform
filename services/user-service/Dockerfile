FROM eclipse-temurin:8-jre-jammy
WORKDIR /app
COPY user-service.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]