
FROM maven:3.9.11-eclipse-temurin-21

WORKDIR /opt/app

# Copy Maven configuration
COPY pom.xml .

# Download dependencies
RUN mvn -B -ntp dependency:go-offline

# Copy application source code
COPY src ./src

# Build Java JAR
RUN mvn -B -ntp clean package

# Create non-root user
RUN groupadd --system globalshop \
    && useradd --system --gid globalshop \
       --home-dir /opt/app globalshop

# Create writable directory for H2 database
RUN mkdir -p /opt/app/data \
    && chown -R globalshop:globalshop /opt/app

# Run application as non-root
USER globalshop

EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8"

ENTRYPOINT ["java", "-jar", "/opt/app/target/globalshop-1.0.0-SNAPSHOT.jar"]
