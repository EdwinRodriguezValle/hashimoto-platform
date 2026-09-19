## 🏗️ Metrics Stack Architecture

*   **Spring Boot Actuator & Micrometer:** Each microservice (`api-gateway` and `patient-service`) collects native core telemetry and exposes it via a Prometheus-compatible format at the `/actuator/prometheus` endpoint.
*   **Prometheus Server (Port 9090):** Functions as the time-series database engine. It executes a scheduled scraping routine every 15 seconds targeting the internal Docker network.
*   **Grafana Labs (Port 3000):** Provides an interactive analytics and visualization layer, natively linked to Prometheus via code-provisioned data sources.

## 🔐 Security Configuration (SecOps)

To guarantee a secure, least-privilege production-ready environment:

*   The Prometheus container executes under a strict non-root user ID standard: `user: "1000:1000"`.
*   Data persistence maps to the host's local directory `./prometheus_data`. Directory ownership must be explicitly adjusted on the physical host by running:  
    `sudo chown -R 1000:1000 ./prometheus_data`
*   Critical telemetry endpoints have been explicitly exempted from OAuth2/Keycloak blocking filters and Java error routing workflows:  
    `.requestMatchers("/actuator/health", "/actuator/prometheus", "/error").permitAll()`

## 🚀 Local Deployment & Verification

1.  Compile the Monorepo applications ensuring the Parent POM inherits all necessary dependencies:
    ```bash
    mvn clean package -DskipTests
    ```
2.  Spin up the entire infrastructure while forcing Docker to rebuild application images:
    ```bash
    docker compose up -d --build
    ```
3.  Verify the scraping targets health by visiting: `http://localhost:9090/targets`. Both services (`api-gateway` and `patient-service`) must display an **UP** status.
4.  Access the visualization web interface at `http://localhost:3000` (Default configured environment credentials: `admin` / `admin`).

## 📊 Curated Community Dashboards Included

To visualize platform performance without manual dashboard modeling, import the following IDs inside Grafana and select the pre-provisioned data source:

*   **JVM Dashboard (Micrometer):** ID `4701` (Tracks Heap/Non-Heap memory allocation, Garbage Collector overhead, and active thread states).
*   **Spring Boot 3.x Statistics:** ID `19011` (Monitors incoming traffic volume, Requests Per Second (RPS), and HTTP response status rate breakdowns per microservice).
    Usa el código con precaución.