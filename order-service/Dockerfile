FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY src src
RUN ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
ARG JAR_FILE=/workspace/build/libs/*.jar
COPY --from=build ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
