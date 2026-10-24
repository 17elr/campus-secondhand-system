FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./
COPY server server
COPY notice-service notice-service
COPY gateway gateway
RUN mvn -B -ntp -Dmaven.test.skip=true package
FROM eclipse-temurin:17-jre
ARG MODULE=server
ARG ARTIFACT=market-service
WORKDIR /app
COPY --from=build /app/${MODULE}/target/${ARTIFACT}-1.0.0-SNAPSHOT.jar app.jar
RUN mkdir /app/uploads && chown -R 10001:10001 /app
USER 10001
ENTRYPOINT ["java","-jar","/app/app.jar"]
