
# Stage 1: Build Java JAR using Maven
FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .

RUN mvn -B -ntp dependency:go-offline

COPY src ./src

RUN mvn -B -ntp clean package


# Stage 2: Runtime image
FROM eclipse-temurin:21-jre

# Create non-root user
RUN groupadd --system globalshop \
    && useradd --system --gid globalshop \
       --home-dir /opt/app globalshop

WORKDIR /opt/app

# Create writable directory for H2 database
RUN mkdir -p /opt/app/data \
    && chown -R globalshop:globalshop /opt/app/data

# Copy application JAR
COPY --from=build --chown=globalshop:globalshop \
    /workspace/target/globalshop-1.0.0-SNAPSHOT.jar app.jar

# Run application as non-root
USER globalshop

EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8"

ENTRYPOINT ["java", "-jar", "/opt/app/app.jar"]
