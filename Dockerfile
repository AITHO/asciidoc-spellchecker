FROM maven:3.8.5-openjdk-17-slim AS builder

ADD ./pom.xml pom.xml
ADD ./src src/

RUN mvn clean package

FROM eclipse-temurin:17-jre-focal
WORKDIR /app
COPY --from=builder target/asciidoctor-spellchecker-*.jar app.jar

CMD ["java", "-jar", "/app/app.jar"]
