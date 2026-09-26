# Hashimoto Platform

Hashimoto Platform is a microservices-based application for patient profile management, identity, observability, and API routing. The stack includes:

- React frontend (Vite + Nginx)
- Spring Boot API Gateway (Spring Cloud Gateway)
- Spring Boot Patient Service (Axon Framework + JPA)
- PostgreSQL (Relational Database)
- Apache Kafka (Event-driven Messaging)
- Keycloak (IAM & Identity Management)
- Prometheus + Grafana (Metrics Stack)
- Elasticsearch + Kibana + Filebeat (Centralized Logging Stack)

This document explains how to run the project locally, which URLs are available, how the platform is structured, and how to handle strict Linux file permissions during co-programming.

## 1. Overview

The platform is composed of several services:

- **Frontend**: React app served by Vite in local development and by Nginx in Docker.
- **API Gateway**: Spring Cloud Gateway, used as the main entry point for backend requests. Handles Token Relay.
- **Patient Service**: Main business service built with Axon Framework for event-driven patient creation and retrieval.
- **Keycloak**: IAM and user authentication provider.
- **PostgreSQL**: Relational database providing persistence for both Keycloak and the Patient Service.
- **Kafka**: Event-driven message broker for Axon Framework events.
- **Prometheus**: Metrics scraping and time-series storage.
- **Grafana**: Visual metrics dashboards.
- **Elasticsearch + Kibana**: Centralized log storage, indexing, and UI exploration.

## 2. Architecture & Data Flow

The application works as follows:

- The user accesses the frontend at `http://localhost:5173`.
- The frontend authenticates against **Keycloak** (`http://localhost:8082`) using PKCE.
- The frontend sends requests containing the Bearer Token to the gateway at `http://localhost:8081`.
- The gateway routes requests to the **Patient Service** (`http://localhost:8080`) and automatically relays the token.
- The Patient Service validates the JWT tokens via an optimized lazy `JwtDecoder` process.
- **PostgreSQL** stores patient entities and Axon event stores.
- **Kafka** handles distributed events.
- **Prometheus** scrapes metrics from Spring Actuator endpoints.
- **Filebeat** runs as root, reads logs from the host, and ships them to **Elasticsearch**.
- **Kibana** is used to explore logs and observability data.

## 3. Services and Access URLs

Once the platform is running, these URLs are available on your host machine:

- **Frontend**: http://localhost:5173
- **API Gateway**: http://localhost:8081
- **Patient Service**: http://localhost:8080
- **Keycloak (Admin Console)**: http://localhost:8082
- **Grafana**: http://localhost:3000
- **Prometheus**: http://localhost:9090
- **Kibana**: http://localhost:5601
- **Elasticsearch**: http://localhost:9200
- **PostgreSQL**: localhost:5432
- **Kafka Broker**: localhost:9092

## 4. Prerequisites

Before starting the project, make sure you have:

- **Docker + Docker Compose** installed.
- **Java 21** installed if you want to run local Spring Boot services from your IDE.
- **Maven** installed.
- **Node.js (v20+) and npm** installed for frontend development.

## 5. Start the Platform (Full-Stack Docker)

From the project root, to build and run all services in containers from A to Z:

```bash
mvn clean package -DskipTests
docker compose up -d --build
```

If you only want to rebuild the frontend and the microservices after code changes:

```bash
docker compose up -d --build frontend api-gateway patient-service
```

## 6. Run the Project Locally During Development

### 6.1 Hybrid Mode: Local Java Services + Docker Infra (Recommended for Coding)

This is the recommended workflow when you are actively coding the Java microservices and want them to run from your IDE or terminal while the backing infrastructure stays in Docker.

1. **Start the Infrastructure Containers:**
   ```bash
   docker compose up -d postgres kafka keycloak elasticsearch kibana filebeat prometheus grafana
   ```

2. **Run the API Gateway (Terminal 1):**
   ```bash
   cd api-gateway
   mvn clean spring-boot:run -Dspring-boot.run.profiles=local
   ```

3. **Run the Patient Service (Terminal 2):**
   ```bash
   cd patient-service
   mvn clean spring-boot:run -Dspring-boot.run.profiles=local
   ```

4. **Run the Frontend in Dev Mode (Terminal 3):**
   ```bash
   cd hashimoto-tracker-app
   npm install
   npm run dev -- --host
   ```

### 6.2 Frontend Only in Vite (Connecting to Docker Backend)

If you prefer to develop just the React app directly from your IDE while the entire backend is up in Docker, run:

```bash
cd hashimoto-tracker-app
npm install
npm run dev -- --host
```
The dev server is configured with a proxy in `vite.config.ts` that intercepts `/api` calls and routes them to `http://localhost:8081` (API Gateway).

## 7. Local Development vs. Docker Profile Strategy

The architecture separates environment properties using Spring Profiles to prevent conflicts regarding token signers and host boundaries.

### Local Profile (`application-local.yml`)
When running from your host machine/IDE:
- **Keycloak Issuer**: `http://localhost:8082/realms/hashimoto-realm`
- **Keycloak JWK Set**: `http://localhost:8082/realms/hashimoto-realm/protocol/openid-connect/certs`
- **PostgreSQL**: `jdbc:postgresql://localhost:5432/patient_db`
- **Kafka**: `localhost:9092`
- **Log Path**: `../logs/api-gateway.log` or `../logs/patient-service.log`

### Docker Environment (`application.yml` Default)
When running inside containers:
- **Keycloak Issuer**: `http://localhost:8082/realms/hashimoto-realm` (Matches the token signed by the physical browser).
- **Keycloak JWK Set**: `http://keycloak:8080/realms/hashimoto-realm/protocol/openid-connect/certs` (Queries Keycloak through the inner bridge network).
- **PostgreSQL**: `jdbc:postgresql://postgres:5432/patient_db`
- **Kafka**: `kafka:29092`
- **Log Path**: `/app/logs/api-gateway.log` or `/app/logs/patient-service.log`

---

## 8. ⚠️ CRITICAL: File Permissions & Co-Programming Bloopers

This project enforces strict security standards, combining non-root users (`1000:1000`), `root` daemons (Filebeat), and host-bound volumes. **If someone clones this repository for the first time, containers or local apps will crash due to Linux permission errors.**

Follow these strict rules to avoid or fix permission blockages:

### 8.1 The Root Cause (Why it breaks)
- When you run the stack in Docker, **Filebeat** runs as `root`. If it creates the `./logs` folder or log files on your host, your local user (`edwin`, `john`, etc.) won't be able to write to them from IntelliJ, throwing a `java.io.FileNotFoundException (Permission denied)`.
- Conversely, if you run microservices inside Docker, they run as `root` or `1000:1000`. If they mount volumes that belong to another owner on the host, they will crash with `mmap-ed active query log: permission denied`.

### 8.2 The Ultimate Permission Reset Script
If any container crashes on startup or your IDE says `Permission denied` on log files, run these commands from the **project root** on your machine:

```bash
# 1. Create the unified logs folder if it doesn't exist
mkdir -p ./logs

# 2. Reclaim ownership of the logs folder for your active local user
sudo chown -R $USER:$USER ./logs

# 3. Grant absolute read/write permissions so Docker (root) and IDEs can coexist
sudo chmod -R 777 ./logs

# 4. Fix Prometheus and Grafana named volume data blockages
sudo chown -R 1000:1000 ./prometheus_data 2>/dev/null || true
```

### 8.3 Ownership Map Reference
- **`./logs` folder**: Shared between your IDE, Docker Backend, and Filebeat. **Must be `chmod 777`**.
- **`./prometheus_data`**: Used by Prometheus container. Must belong to UID `1000:1000`.
- **`./init-scripts`**: Read-only entrypoint for PostgreSQL. Needs read permissions (`chmod +r`).

---

## 9. Logging Strategy & Centralization

To keep the repository perfectly clean, all duplicated log directories (`app/logs`, `hashimoto_logs`, `patient_logs`) have been deprecated.

### Unified Structure

```text
hashimoto-platform/
├── logs/                      <-- 📁 SINGLE SOURCE OF TRUTH (Ignored in .gitignore)
│   ├── api-gateway.log
│   └── patient-service.log
├── docker-compose.yaml
├── filebeat.yml
├── api-gateway/
├── patient-service/
└── hashimoto-tracker-app/
```

- **Local Mode**: Logs write directly outwards to `../logs/`.
- **Docker Mode**: Logs write to `/app/logs/`.
- **Docker Volume Mount**: `./logs:/app/logs` links both worlds seamlessly.
- **Filebeat**: Mounts `./logs:/app/logs` as well and reads strictly from `/app/logs/*.log`.

## 10. Filebeat & Java Multiline Management

Filebeat is configured using a custom `filestream` tracking engine. It includes a built-in regex parser to ensure that Spring Boot `NullPointerExceptions` and Axon Framework stack traces are **not split into separate lines** inside Kibana, keeping logs intact.

```yaml
parsers:
  - multiline:
      type: pattern
      pattern: '^[0-9]{4}-[0-9]{2}-[0-9]{2}' # Detects ISO timestamps
      negate: true
      match: after
```

## 11. Database Initialization (The Keycloak Trap)
The official PostgreSQL Docker image **only creates one database** automatically via `POSTGRES_DB: patient_db`.
Because Keycloak requires its own isolated database (`keycloak_db`), this repository mounts a bootstrap script:

- **Location**: `./init-scripts/init.sql`
- **Action**: Runs automatically on the first container startup to verify and execute `CREATE DATABASE keycloak_db;`. 
