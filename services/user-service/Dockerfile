FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mkdir -p /root/.m2 \
    && printf '%s\n' \
      '<settings><mirrors><mirror>' \
      '<id>aliyun</id><mirrorOf>*</mirrorOf>' \
      '<url>https://maven.aliyun.com/repository/public</url>' \
      '</mirror></mirrors></settings>' \
      > /root/.m2/settings.xml \
    && mvn -B -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/target/user-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
