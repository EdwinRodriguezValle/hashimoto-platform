## 🌐 Exposed Ports & Interactive Entry Points

When the platform is running locally via Docker Compose, all core infrastructure tools, administration consoles, and gateway endpoints are exposed on your host machine. You can click directly on the links below to access them:

### 👤 Core Application & Gateway Interfaces
*   **API Gateway (Perimeter Router):** [http://localhost:8081](http://localhost:8081) — Central reactive entry point. All microservices traffic (e.g., `/api/patients/**`) routes through here.
*   **Patient Core Service:** [http://localhost:8080](http://localhost:8080) — Core backend microservice. Directly exposes Spring Actuator metrics locally.

### 🔐 Identity & Access Management (IAM)
*   **Keycloak Server:** [http://localhost:8082](http://localhost:8082) — Corporate Identity Administration console. Manages user records, RBAC roles, and token signing for the `hashimoto-realm`.

### 📊 Performance Metrics & Telemetry (Prometheus + Grafana Stack)
*   **Grafana Dashboards:** [http://localhost:3000](http://localhost:3000) — Analytics visualization web ui. Use credentials `admin` / `admin` to view JVM and Spring Boot health.
*   **Prometheus Engine:** [http://localhost:9090](http://localhost:9090) — Time-series database dashboard. Access [http://localhost:9090/targets](http://localhost:9090/targets) to check scrapers status.

### 🔍 Centralized Log Aggregation (ELK Stack)
*   **Kibana Console:** [http://localhost:5601](http://localhost:5601) — Operational log explorer dashboard. Map your Data Views (`hashimoto-logs-*`) here to trace live logs.
*   **Elasticsearch Cluster:** [http://localhost:9200](http://localhost:9200) — High-performance text indexing datastore engine.

### 🗄️ Backend Infrastructure (Database & Event Streaming)
*   **PostgreSQL Engine:** `localhost:5432` — Unified relational server hosting isolated database schemas: `patient_db` and `keycloak_db`.
*   **Apache Kafka Broker:** `localhost:9092` — Event-driven asynchronous messaging cluster managing the `patient-events` topics.
