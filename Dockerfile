FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN ./mvnw -q -e -DskipTests package

CMD ["java", "-jar", "target/quiz-bot-java-1.0-SNAPSHOT.jar"]
