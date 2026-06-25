ARG DOCKERHUB_LIBRARY_PREFIX=

FROM ${DOCKERHUB_LIBRARY_PREFIX}maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace/dealer-server

COPY docker/maven/settings.xml /root/.m2/settings.xml
COPY dealer-server/pom.xml ./

RUN mvn -B -s /root/.m2/settings.xml -DskipTests dependency:go-offline

COPY dealer-server/src src

RUN mvn -B -s /root/.m2/settings.xml -DskipTests package

FROM ${DOCKERHUB_LIBRARY_PREFIX}eclipse-temurin:17-jre
WORKDIR /app

ENV TZ=Asia/Shanghai

COPY --from=build /workspace/dealer-server/target/dealer-server-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8089

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
