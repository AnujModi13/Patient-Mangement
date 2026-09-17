FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

# 1. Copy POM files for both services
COPY billing-service/pom.xml ./billing-service/pom.xml
COPY patient-service/pom.xml ./patient-service/pom.xml

# 2. Copy source code for both services
COPY billing-service/src ./billing-service/src
COPY patient-service/src ./patient-service/src

# 3. Build & install billing-service into the container's local .m2 cache
RUN mvn clean install -DskipTests -f billing-service/pom.xml

# 4. Build patient-service now that billing-service SNAPSHOT is installed
RUN mvn clean package -DskipTests -f patient-service/pom.xml

# Runtime Stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/patient-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]