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

For the Keycloak setup across hybrid local development, Compose, and Kubernetes, see [Keycloak architecture and configuration](docs/keycloak.md).

## 1. Overview

The platform is composed of several services:

- **Frontend**: React app served by Vite in local development and by Nginx in Docker.
- **API Gateway**: Spring Cloud Gateway, used as the main entry point for backend requests. Validates access-token signature, issuer, and API audience before routing.
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
- The gateway forwards the incoming `Authorization: Bearer` header to **Patient Service** (`http://localhost:8080`).
- Both the gateway and Patient Service validate the JWT signature, issuer, and `api-gateway-client` audience; Patient Service also enforces the patient roles.
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

The backend services require **JDK 21**. If you use SDKMAN and have multiple Java versions installed, select the project version in each terminal before starting Maven:

```bash
sdk use java 21.0.9-tem
java -version
mvn -version
```

Both version commands should report Java 21. This only changes Java for the current terminal; repeat it in each terminal where you start a Java service. If that SDKMAN version is not installed, run `sdk install java 21.0.9-tem` once.

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
The dev server proxies `/api` to `http://localhost:8081` (API Gateway) and `/auth` to Keycloak at `http://localhost:8082`.

## 7. Local Development vs. Docker Profile Strategy

The architecture separates environment properties using Spring Profiles and environment variables. Keep the token issuer stable within each environment; configure the JWKS address separately so each backend uses an address reachable from its own network.

Both backends also require `API_AUDIENCE=api-gateway-client`. Keycloak adds that audience to access tokens issued to `hashimoto-tracker-web`. After changing the Keycloak mapper, sign out and back in to receive a token with the updated `aud` claim.

### Local Profile (`application-local.yml`)
When running from your host machine/IDE:
- **Keycloak Issuer**: `http://localhost:8082/realms/hashimoto-realm`
- **Keycloak JWK Set**: `http://localhost:8082/realms/hashimoto-realm/protocol/openid-connect/certs`
- **PostgreSQL**: `jdbc:postgresql://localhost:5432/patient_db`
- **Kafka**: `localhost:9092`
- **Log Path**: `../logs/api-gateway.log` or `../logs/patient-service.log`

### Docker Environment (`application.yml` Default)
When running inside containers:
- **Keycloak Issuer**: `http://localhost:8082/realms/hashimoto-realm` (The public issuer included in tokens).
- **Keycloak JWK Set**: `http://keycloak:8080/realms/hashimoto-realm/protocol/openid-connect/certs` (Compose DNS, reachable from backend containers).
- **PostgreSQL**: `jdbc:postgresql://postgres:5432/patient_db`
- **Kafka**: `kafka:29092`
- **Log Path**: `/app/logs/api-gateway.log` or `/app/logs/patient-service.log`

### Kubernetes Local
- **Keycloak Issuer**: `http://localhost:8085/auth/realms/hashimoto-realm`
- **Keycloak JWK Set**: `http://keycloak-service.hashimoto-infra.svc.cluster.local:8080/auth/realms/hashimoto-realm/protocol/openid-connect/certs`
- Open the application at `http://localhost:8085/`. Using `localhost` keeps the browser in a secure context for Web Crypto and OIDC PKCE.

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

## 11. PostgreSQL databases
The local PostgreSQL instance hosts one database per service: `patient_db` for Patient Service (including Axon and its read projection), and `keycloak_db` for Keycloak. `POSTGRES_DB` creates `patient_db`; the initialization script creates `keycloak_db`:

- **Location**: `./init-scripts/init.sql`
- **Action**: Runs automatically on the first startup of an empty PostgreSQL data volume and executes `CREATE DATABASE keycloak_db;`.
- **Important**: Initialization scripts do not run again when a volume already contains a database. Changing this script does not migrate existing volumes.

## Run the Kubernetes development cluster

Follow the single, maintained [K3d onboarding guide](docs/k8s-onboarding.md). It creates the cluster with the Traefik Ingress controller expected by the manifests, builds/imports the application images, applies infrastructure configuration, and deploys the stack in dependency order.

