FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY member-service/build.gradle.kts member-service/
COPY member-service/src member-service/src
RUN ./gradlew :member-service:bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
ARG JAR_FILE=/workspace/member-service/build/libs/*.jar
COPY --from=build ${JAR_FILE} app.jar
COPY whatap_apm /whatap
ENTRYPOINT ["java", "-javaagent:/whatap/whatap.agent-2.2.71.jar", "-Dwhatap.oname=member", "-jar", "/app.jar"]
