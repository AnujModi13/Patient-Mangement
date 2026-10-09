# Patient Management System (Microservices Architecture)

A production-grade, cloud-native Patient Management System built with **Java 17** and **Spring Boot 3**. This project demonstrates a transition from functional backend paradigms (Node.js/Flask) to a robust **Object-Oriented Microservices Architecture** featuring gRPC inter-service communication, event-driven messaging with Apache Kafka, centralized authentication via Spring Security & JWT, OpenAPI documentation, and automated IaC cloud deployment to AWS using **LocalStack**.

Inspired by Chris Blakely’s *Build & Deploy a Production-Ready Patient Management System with Microservices: Java Spring Boot AWS*.

---

## 🏗️ Architecture Overview

```
                                      +-----------------------+
                                      |   Client / Postman    |
                                      +-----------+-----------+
                                                  |
                                                  v
                                      +-----------------------+
                                      |   Spring Cloud Gateway| (JWT Auth Filter)
                                      +---+---------------+---+
                                          |               |
                    +---------------------+               +---------------------+
                    |                                                           |
                    v                                                           v
        +-----------------------+                                   +-----------------------+
        |     Patient Service   |                                   |     Auth Service      |
        | (REST / Kafka / gRPC) |                                   | (Spring Security / JWT|
        +---+---------------+---+                                   +-----------+-----------+
            |               |                                                   |
 (gRPC Sync)|               |(Kafka Async)                                      v
            v               v                                           +---------------+
  +-----------------+   +--------------------+                          | Auth Postgres |
  | Billing Service |   | Analytics Service  |                          +---------------+
  |  (gRPC Server)  |   |  (Kafka Consumer)  |
  +--------+--------+   +---------+----------+
           |                      |
           v                      v
  +-----------------+   +--------------------+
  | Billing Postgres|   | Analytics Postgres |
  +-----------------+   +--------------------+
```

### Key Architectural Highlights
* **Microservices Framework:** Multi-module Java/Spring Boot services with domain isolation.
* **Inter-Service Communication:** Synchronous gRPC via Protobuf for internal service calls; Asynchronous event-driven publishing/subscribing via Apache Kafka.
* **API Gateway & Auth:** Centralized routing with Spring Cloud Gateway; Bearer JWT token verification via Auth Service.
* **DevOps & Cloud:** Full containerization with Docker & Docker Compose; IaC CloudFormation deployment targeting AWS LocalStack emulation.

---

## 🛠️ Microservices Breakdown

| Service Name | Description | Key Tech Stack |
| :--- | :--- | :--- |
| **`api-gateway`** | Central entry point for external traffic. Routes requests and applies JWT authentication filters. | Spring Cloud Gateway, Java 17 |
| **`auth-service`** | Issues and validates JWT tokens. Manages user registration, login, and Spring Security rules. | Spring Security, JJWT, PostgreSQL, H2 |
| **`patient-service`** | Core service managing patient records. Invokes gRPC calls to Billing Service and publishes events to Kafka. | Spring Data JPA, gRPC Client, Kafka Producer, PostgreSQL |
| **`billing-service`** | High-performance gRPC server handling patient billing and invoice generations. | gRPC Server, Netty, Protobuf, PostgreSQL |
| **`analytics-service`** / **`notification-service`** | Consumer listening to patient-related events emitted on Kafka for telemetry and logging. | Spring Kafka Consumer, Protobuf |

---

## ⚙️ Environment Variables & Configuration Reference

Below are all the configuration variables required to run each component locally or in containerized environments.

### 1. Patient Service
```env
BILLING_SERVICE_ADDRESS=billing-service
BILLING_SERVICE_GRPC_PORT=9005
JAVA_TOOL_OPTIONS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
SPRING_DATASOURCE_PASSWORD=password
SPRING_DATASOURCE_URL=jdbc:postgresql://patient-service-db:5432/db
SPRING_DATASOURCE_USERNAME=admin_user
SPRING_JPA_HIBERNATE_DDL_AUTO=update
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
SPRING_SQL_INIT_MODE=always
```
*`application.properties` (Kafka Config):*
```properties
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.ByteArrayDeserializer
```

---

### 2. Billing Service
```env
JAVA_TOOL_OPTIONS=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005
SPRING_DATASOURCE_PASSWORD=password
SPRING_DATASOURCE_URL=jdbc:postgresql://billing-service-db:5432/db
SPRING_DATASOURCE_USERNAME=admin_user
SPRING_JPA_HIBERNATE_DDL_AUTO=update
SPRING_SQL_INIT_MODE=always
```

---

### 3. Auth Service & Auth Database

**Auth Service Environment Variables:**
```env
SPRING_DATASOURCE_PASSWORD=password
SPRING_DATASOURCE_URL=jdbc:postgresql://auth-service-db:5432/db
SPRING_DATASOURCE_USERNAME=admin_user
SPRING_JPA_HIBERNATE_DDL_AUTO=update
SPRING_SQL_INIT_MODE=always
```

**Auth Postgres Container Environment Variables:**
```env
POSTGRES_DB=db
POSTGRES_PASSWORD=password
POSTGRES_USER=admin_user
```

---

### 4. Notification / Analytics Service
```env
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
```

---

### 5. Kafka Broker Configuration
When running the Kafka broker locally or inside Docker/IntelliJ:
```env
KAFKA_CFG_ADVERTISED_LISTENERS=PLAINTEXT://kafka:9092,EXTERNAL://localhost:9094
KAFKA_CFG_CONTROLLER_LISTENER_NAMES=CONTROLLER
KAFKA_CFG_CONTROLLER_QUORUM_VOTERS=0@kafka:9093
KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,EXTERNAL:PLAINTEXT,PLAINTEXT:PLAINTEXT
KAFKA_CFG_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093,EXTERNAL://:9094
KAFKA_CFG_NODE_ID=0
KAFKA_CFG_PROCESS_ROLES=controller,broker
```

---

## 📦 Dependencies & gRPC Build Setup (`pom.xml`)

For services using gRPC (`billing-service`, `patient-service`), include the following gRPC and Protobuf dependencies and build configuration:

```xml
<!-- Dependencies -->
<dependencies>
    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-netty-shaded</artifactId>
        <version>1.69.0</version>
    </dependency>
    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-protobuf</artifactId>
        <version>1.69.0</version>
    </dependency>
    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-stub</artifactId>
        <version>1.69.0</version>
    </dependency>
    <dependency>
        <groupId>net.devh</groupId>
        <artifactId>grpc-spring-boot-starter</artifactId>
        <version>3.1.0.RELEASE</version>
    </dependency>
    <dependency>
        <groupId>com.google.protobuf</groupId>
        <artifactId>protobuf-java</artifactId>
        <version>4.29.1</version>
    </dependency>
</dependencies>

<!-- Build Plugin Configuration -->
<build>
    <extensions>
        <extension>
            <groupId>kr.motd.maven</groupId>
            <artifactId>os-maven-plugin</artifactId>
            <version>1.7.0</version>
        </extension>
    </extensions>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
        <plugin>
            <groupId>org.xolstice.maven.plugins</groupId>
            <artifactId>protobuf-maven-plugin</artifactId>
            <version>0.6.1</version>
            <configuration>
                <protocArtifact>com.google.protobuf:protoc:3.25.5:exe:${os.detected.classifier}</protocArtifact>
                <pluginId>grpc-java</pluginId>
                <pluginArtifact>io.grpc:protoc-gen-grpc-java:1.68.1:exe:${os.detected.classifier}</pluginArtifact>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>compile</goal>
                        <goal>compile-custom</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

---

## 🚀 Getting Started

### Prerequisites
* **Java 17+**
* **Maven 3.8+**
* **Docker & Docker Desktop**
* **AWS CLI & LocalStack**

### Quick Start with Docker Compose
1. Clone the repository:
   ```bash
   git clone https://github.com/AnujModi13/Patient-Mangement.git
   cd Patient-Mangement
   ```
2. Build Maven binaries:
   ```bash
   mvn clean package -DskipTests
   ```
3. Start up the complete infrastructure:
   ```bash
   docker-compose up --build -d
   ```

---

## ☁️ LocalStack AWS Deployment

Deploy infrastructure stacks locally using AWS CloudFormation and LocalStack:

1. **Start LocalStack:**
   ```bash
   localstack start -d
   ```
2. **Deploy CloudFormation Stacks via AWS CLI:**
   ```bash
   # Create VPC
   aws --endpoint-url=http://localhost:4566 cloudformation create-stack \
     --stack-name patient-management-vpc \
     --template-body file://infrastructure/vpc.yml

   # Create Databases
   aws --endpoint-url=http://localhost:4566 cloudformation create-stack \
     --stack-name patient-management-db \
     --template-body file://infrastructure/databases.yml

   # Create ECS & MSK Clusters
   aws --endpoint-url=http://localhost:4566 cloudformation create-stack \
     --stack-name patient-management-ecs \
     --template-body file://infrastructure/ecs-services.yml
   ```

---

## 🔗 References & Credits

* GitHub Repository: [AnujModi13/Patient-Mangement](https://github.com/AnujModi13/Patient-Mangement)
* Video Guide: [Build & deploy a production ready patient management system with Microservices by Chris Blakely](https://www.youtube.com/watch?v=tseqdcFfTUY)
