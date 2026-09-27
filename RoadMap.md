# 🗺️ Engineering Roadmap: Hashimoto Tracker (MVP Sandbox)

This document outlines the 3-week strategic engineering plan to transition the current Monorepo from a local Docker Compose setup into an enterprise-grade ecosystem orchestrated in **Kubernetes** utilizing **CI/CD** pipelines and **GitOps (ArgoCD)** methodologies.

## 🎯 Current Status (Completed)
* [x] Core Microservices Architecture using Spring Boot 3.3.1 + Axon Framework (CQRS/Event Sourcing).
* [x] Persistent storage layer (PostgreSQL) and reactive event streaming (Apache Kafka).
* [x] Identity and Access Management (IAM) server (Keycloak) with automated realm importing configuration.
* [x] SecOps: Strict elimination of `root` privileges inside containers (Standardized `user: "1000:1000"` execution).
* [x] Observability Pillar 1: Centralized log aggregation via ELK Stack (Filebeat + Elasticsearch + Kibana).
* [x] Observability Pillar 2: Hardware & JVM real-time metrics telemetry via Prometheus + Grafana (Automated data source provisioning).
* [x] MVP Frontend: React + Vite + TypeScript application fully integrated with Keycloak PKCE and working End-to-End.

---

## 📅 WEEK 2: Migrating from Docker Compose to Local Kubernetes
**Objective:** Deprecate flat multi-container runtimes and master cloud-native container orchestration on Ubuntu.

*   [ ] **Day 8-9: Cluster Provisioning & Ingress Setup**
    *   [ ] Install K3d (Rancher-backed) or Minikube on the local Ubuntu host.
    *   [ ] Deploy an Ingress Controller (Nginx Ingress) to centralize all incoming external network traffic through a single host port.
*   [ ] **Day 10-12: K8s Manifest Declarations (`/k8s` Directory)**
    *   [ ] Create a structured `/k8s` directory in the root of the Monorepo.
    *   [ ] Write `StatefulSets` and `PersistentVolumeClaims (PVC)` to guarantee stateful persistence for Postgres, Kafka, and Elasticsearch.
    *   [ ] Write stateless `Deployments` and `Services` (ClusterIP type) for `api-gateway`, `patient-service`, `frontend`, and `keycloak`.
*   [ ] **Day 13-14: Internal Networking & Health Configurations**
    *   [ ] Externalize runtime parameters via Kubernetes `ConfigMaps` and Encrypted `Secrets`.
    *   [ ] Validate internal service discovery via K8s native CoreDNS (e.g., routing backend to `http://postgres:5432`).
    *   [ ] Configure `Liveness` and `Readiness Probes` bound directly to Spring Boot Actuator endpoints.

## 📅 WEEK 3: Conquering GitOps (ArgoCD) & Production Environment Simulation
**Objective:** Implement standard industry declarative continuous delivery models for automated reconciliation loops.

*   [ ] **Day 15-16: ArgoCD Deployment**
    *   [ ] Install the ArgoCD Operator core components within the Kubernetes cluster (`namespace: argocd`).
    *   [ ] Expose the ArgoCD Web UI and map a secure webhook/connection to the remote GitHub Repository.
*   [ ] **Day 17-19: Fully Automated GitOps Pipeline (CD)**
    *   [ ] Declare an ArgoCD custom application resource (`Application CRD`) tracking the `/k8s` directory manifests.
    *   [ ] Enable *Auto-Sync* policies accompanied by *Prune* and *Self-Heal* capabilities.
    *   [ ] Refactor the Week 1 GitHub Actions pipeline to automatically patch image tags within K8s manifests upon successful builds, triggering an automated ArgoCD deployment rollout.
*   [ ] **Day 20-21: Kubernetes Infrastructure Observability & Final Milestone Review**
    *   [ ] Migrate Prometheus and Grafana from Docker to K8s utilizing **Helm Charts** or the **Prometheus-Operator**.
    *   [ ] Import advanced Kubernetes cluster performance Dashboards to monitor Pod resource consumption (CPU/RAM).
    *   [ ] Final Proof of Concept: Push a code change -> Watch CI build on GitHub -> Witness ArgoCD update the K8s cluster -> Validate results live on the Frontend interface.
