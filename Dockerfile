FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/demo-kafka-1.0.0.jar app.jar

EXPOSE 8888

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
