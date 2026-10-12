# Hashimoto Platform

Hashimoto is an early-stage health-tracking platform for people living with Hashimoto's disease and other immune conditions. The product direction is to help people record symptoms, routines, medication, food, and other daily factors, then review their history and possible patterns.

The repository contains the current development platform and a roadmap for future product capabilities. It is not a clinical decision system, and the architecture diagrams do not mean every future service or integration has been implemented.

## Current status

The current application foundation includes:

- **Web app:** React, TypeScript, and Vite; Nginx serves the production container image.
- **API Gateway:** Spring Boot and Spring Cloud Gateway. It validates access-token signature, issuer, and audience before routing API requests.
- **Patient Service:** Spring Boot with Axon/JPA for the current patient create/read workflow. It validates tokens and enforces patient roles.
- **Identity:** Keycloak handles sign-in, registration, and access tokens using OpenID Connect Authorization Code with PKCE.
- **Data and messaging:** PostgreSQL and Kafka.
- **Observability:** Prometheus/Grafana for metrics and Filebeat/Elasticsearch/Kibana for logs.
- **Local deployment:** Docker Compose for a one-machine stack and K3d/Kubernetes manifests for the local cluster.

The symptom, routine, meal, and medication-tracking workflows are still product work to define and build. See the [12-month roadmap](RoadMap.md) and the [target architecture](docs/fullArchitectureHashimoto%20.md) for future plans.

## Choose how to run it

| Mode | Application processes | Start here |
| --- | --- | --- |
| **Full Docker Compose** | Frontend, Gateway, Patient Service, and infrastructure run in containers | [Quick start](#quick-start-full-docker-compose) |
| **Hybrid development** | Frontend and Java services run on the host; PostgreSQL, Kafka, Keycloak, and observability run in Docker | [Hybrid development](#hybrid-development-local-services--docker-infrastructure) |
| **Kubernetes on K3d** | The platform runs in the local K3d cluster | [K3d onboarding](docs/k8s-onboarding.md) |

To start GitOps on a new branch, follow [Argo CD local onboarding](docs/argocd-onboarding.md). The initial Argo CD Application manages only `k8s/core`.

Use one mode at a time. Hybrid services and full Compose expose the same host ports, so stop one mode's application processes before starting the other.

## Quick start: full Docker Compose

### Requirements

- Docker Engine with the Docker Compose plugin.
- Enough local memory for PostgreSQL, Kafka, Keycloak, Elasticsearch, Kibana, Prometheus, Grafana, and the application containers. The full observability stack is resource intensive.

### 1. Configure local development variables

Copy the sample file once:

```bash
cp .env.example .env
```

`.env.example` contains development-only values. Keep `.env` private and do not use those passwords in a shared or production environment.

### 2. Build and start the stack

From the repository root:

```bash
docker compose config --quiet
docker compose up -d --build
docker compose ps
```

The first build downloads base images and dependencies and may take several minutes. Wait for PostgreSQL, Keycloak, and Elasticsearch to report healthy before using the application.

### 3. Open the services

| Service | Local URL |
| --- | --- |
| Hashimoto web app | [http://localhost:5173/](http://localhost:5173/) |
| Keycloak Admin Console | [http://localhost:8082/admin/](http://localhost:8082/admin/) |
| API Gateway health | [http://localhost:8081/actuator/health](http://localhost:8081/actuator/health) |
| Patient Service health | [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health) |
| Prometheus | [http://localhost:9090/](http://localhost:9090/) |
| Grafana | [http://localhost:3000/](http://localhost:3000/) |
| Elasticsearch | [http://localhost:9200/](http://localhost:9200/) |
| Kibana | [http://localhost:5601/](http://localhost:5601/) |
| PostgreSQL | `localhost:5432` |
| Kafka host listener | `localhost:9092` |

Keycloak's local Compose administrator is `admin` / `admin`. This is for local development only. In the `hashimoto-realm`, register an account through the app, then have an administrator add it to the `patient-clerks` group to grant the patient roles. See [Keycloak user and role workflow](docs/keycloak-user-workflow.md).

The API is protected. A request without a valid access token should receive `401`; a valid user without the required role should receive `403` for a role-protected operation.

### Stop the stack and manage its data

```bash
docker compose down
```

This stops and removes the containers but preserves named volumes. `docker compose down -v` also deletes the local database, Kafka, metrics, and Elasticsearch data; use it only when you intentionally want a full reset.

Useful log command:

```bash
docker compose logs -f api-gateway patient-service keycloak
```

## Hybrid development: local services + Docker infrastructure

Use this mode when editing Java or frontend code. The Java processes and Vite run on the host, while their dependencies stay in Docker.

### Requirements

- Docker Engine and Compose.
- JDK 21 and Maven for the Spring Boot services.
- Node.js **20.19+** (or **22.12+**) and npm for the Vite frontend.

If you use SDKMAN with more than one Java installed, select Java 21 in every terminal that starts a Java service:

```bash
sdk use java 21.0.9-tem
java -version
mvn -version
```

Both version commands should report Java 21. This selection applies only to the current terminal. Install that SDKMAN version once with `sdk install java 21.0.9-tem` if it is not present; alternatively, configure another installed JDK 21 in your IDE or `JAVA_HOME`.

### 1. Start only the infrastructure containers

From the repository root, make sure `.env` exists as described above, then run:

```bash
mkdir -p logs
docker compose up -d postgres kafka keycloak elasticsearch kibana filebeat prometheus grafana
```

Creating `logs/` first keeps it owned by your local account for Java processes running on the host. If you previously ran the full Compose application and its log files are root-owned, restore ownership of this generated directory before starting local Java services: `sudo chown -R "$(id -u):$(id -g)" logs`.

### 2. Start API Gateway and Patient Service

Open separate terminals. In each Java terminal, select JDK 21 first if you use SDKMAN.

Terminal 1:

```bash
cd api-gateway
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Terminal 2:

```bash
cd patient-service
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### 3. Start the frontend

In another terminal:

```bash
cd hashimoto-tracker-app
npm ci
npm run dev -- --host
```

Open [http://localhost:5173/](http://localhost:5173/). Vite proxies `/api` to the local API Gateway and `/auth` to Keycloak in Compose. The browser stays on `localhost`, which supports Web Crypto required by the OIDC PKCE flow.

## Kubernetes on K3d

The K3d manifests deploy the current application and infrastructure to a local Kubernetes cluster. Follow the maintained [K3d onboarding guide](docs/k8s-onboarding.md) for host requirements, the `hashimoto.local` host mapping, cluster creation, image builds/imports, deployment order, and checks. Use `http://localhost:8085/` as the browser URL for the Kubernetes OIDC flow.

Read [Kubernetes architecture](docs/k8s-architecture.md) for namespaces, services, storage, ingress, and Kafka networking. Read [Keycloak architecture and configuration](docs/keycloak.md) for issuer/JWKS URLs and identity setup across Compose, hybrid, and Kubernetes.

## Security and data boundaries

- `hashimoto-tracker-web` is a public OIDC client and uses PKCE; it contains no client secret.
- The API Gateway validates the access token's signature, issuer, and `api-gateway-client` audience. Patient Service validates it again and authorizes patient operations using Keycloak roles.
- Keycloak stores identities and credentials in `keycloak_db`. Patient Service stores patient data and its Axon/read-model data in `patient_db`. These are separate databases on the current local PostgreSQL server.
- Self-registration does not grant business roles automatically. An administrator manages patient permissions through the `patient-clerks` group.
- The current setup is for local development. Its sample passwords, single-node infrastructure, and local storage are not production settings.

Health information is sensitive. Do not send identifiable data to external analytics or AI providers until privacy, security, clinical-safety, and provider requirements have been reviewed. The future provider integrations are described in the [target architecture](docs/fullArchitectureHashimoto%20.md) and [roadmap](RoadMap.md); they are not implemented in the current app.

## Repository map

```text
api-gateway/             Spring Cloud Gateway and token validation
patient-service/         Patient API, domain logic, and persistence
hashimoto-tracker-app/   React/Vite frontend
k8s/                     K3d/Kubernetes manifests
init-scripts/            PostgreSQL first-start initialization
docs/                    Architecture, Keycloak, and onboarding guides
RoadMap.md               Tentative 12-month product and engineering plan
Makefile                 Local K3d build and deployment commands
docker-compose.yaml      Full local Docker development stack
```

Generated logs are stored under `./logs/` and excluded from Git. Do not use blanket `chmod 777` permissions. If a local permission error occurs, inspect the owner and mode of the specific file or directory and fix only that path.

## Documentation

- [Tentative 12-month roadmap](RoadMap.md)
- [Target application architecture](docs/fullArchitectureHashimoto%20.md)
- [Kubernetes architecture](docs/k8s-architecture.md)
- [K3d onboarding](docs/k8s-onboarding.md)
- [Logs and metrics guide](docs/observability.md)
- [Keycloak architecture and configuration](docs/keycloak.md)
- [Keycloak user and role workflow](docs/keycloak-user-workflow.md)
