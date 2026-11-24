FROM eclipse-temurin:21-jre

COPY build/libs/app.jar app.jar
CMD ["java", "-jar", "app.jar"]
