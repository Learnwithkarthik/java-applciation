# Build a real Java JAR with Maven; ship only the runtime, not Maven or the source.
FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp clean package

FROM eclipse-temurin:21-jre
RUN groupadd --system globalshop && useradd --system --gid globalshop --home-dir /opt/app globalshop
WORKDIR /opt/app
COPY --from=build --chown=globalshop:globalshop /workspace/target/globalshop-1.0.0-SNAPSHOT.jar app.jar
USER globalshop
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8"
ENTRYPOINT ["java", "-jar", "/opt/app/app.jar"]
