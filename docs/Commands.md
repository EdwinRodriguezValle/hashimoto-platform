# 🛠️ DevOps & Development Cheat Sheet

A comprehensive collection of the most frequently used commands across the Hashimoto Platform ecosystem.

## 🐳 Docker Compose Lifecycle
Execute these commands from the root directory where the `docker-compose.yaml` is located.

*   **Start the entire platform in background:**
    `docker compose up -d`
*   **Stop all running containers without losing data:**
    `docker compose stop`
*   **Destroy all containers and release internal networks:**
    `docker compose down`
*   **Force rebuild and restart specific microservices (after code changes):**
    `docker compose up -d --build api-gateway patient-service`
*   **Wipe out a corrupted container along with its anonymous volumes:**
    `docker compose rm -fvs <service_name>`

## 🔍 Observability & Troubleshooting (Logs)
*   **Stream live application logs from a single container:**
    `docker logs -f <container_name>` (e.g., `docker logs -f prometheus`)
*   **Check system resource usage of all Docker containers:**
    `docker stats`

## ☕ Java & Maven Monorepo Management
*   **Clean and build all microservices (skipping tests for speed):**
    `mvn clean package -DskipTests`
*   **Force update snapshots/dependencies from Maven Central:**
    `mvn clean package -U -DskipTests`

## 🔐 Host Permission Fixes (SecOps)
*   **Fix Prometheus storage write access on Ubuntu host:**
    `sudo chown -R 1000:1000 ./prometheus_data`
*   **Fix Filebeat configuration security compliance requirements:**
    `sudo chown root ./filebeat.yml`