# Logs and metrics: developer guide

Hashimoto has two separate observability paths:

- **Logs:** Filebeat collects application/container logs, Elasticsearch indexes them, and Kibana lets developers search them.
- **Metrics:** Prometheus scrapes Spring Boot Actuator metrics, and Grafana queries Prometheus to display them.

These tools answer different questions. Use logs to inspect a particular request or error; use metrics to see rates, latency, JVM and process health over time. The current setup is for local development and learning, not a production observability platform.

## Docker Compose and hybrid development

Start the dependencies from the repository root:

```bash
mkdir -p logs
docker compose up -d elasticsearch kibana filebeat prometheus grafana
```

To collect logs from the Java services, they must also be running. With full Compose, start the complete application using the [root README](../README.md#quick-start-full-docker-compose). In hybrid mode, start the Java services with the `local` Spring profile; they write to the shared `./logs/` directory and Filebeat reads `*.log` from that directory. The local profile gives Gateway and Patient Service separate log files.

| Tool | Local URL | Purpose |
| --- | --- | --- |
| Kibana | [http://localhost:5601](http://localhost:5601) | Search and filter indexed logs |
| Elasticsearch | [http://localhost:9200](http://localhost:9200) | Log index API and cluster health |
| Prometheus | [http://localhost:9090](http://localhost:9090) | Inspect targets and query metrics |
| Grafana | [http://localhost:3000](http://localhost:3000) | Build and view metric dashboards |

### Search application logs in Kibana

1. Open **Stack Management → Data Views** and create a data view with `logs-*` as its index pattern.
2. Select `@timestamp` as the time field.
3. Open **Discover**, choose the data view, set a suitable time range, and search/filter fields such as `message`.

Compose Filebeat reads `/app/logs/*.log` from the shared logs volume and sends events to Elasticsearch as daily indices named `logs-YYYY.MM.DD`. It also groups Java multiline stack traces. For example, search `patient-service` or `api-gateway` in the message or log path fields. Filebeat only harvests files that exist in its configured path; it does not automatically collect every container's stdout in Compose.

### Query metrics in Prometheus and Grafana

Open Prometheus **Status → Targets** and confirm that the `prometheus` and `hashimoto-apps` targets are `UP`. The application scrape endpoints are:

- `patient-service:8080/actuator/prometheus`
- `api-gateway:8081/actuator/prometheus`

Try a simple PromQL query such as `up` in Prometheus' Graph page. Grafana is provisioned with Prometheus as its data source (`http://prometheus:9090`) and loads the **Hashimoto Platform Overview** dashboard automatically. The starter dashboard shows application scrape availability, HTTP request rate, mean server response time, and JVM heap usage. The latency panel shows a mean, not p95/p99; percentile panels require histogram buckets to be enabled and verified. Alert rules and notification channels are not configured yet.

Useful Compose commands:

```bash
docker compose ps filebeat elasticsearch kibana prometheus grafana
docker compose logs -f filebeat elasticsearch kibana
docker compose logs -f prometheus grafana
```

If Kibana shows no events, confirm the app has written files beneath `./logs/`, check Filebeat logs for harvest/output errors, and confirm Elasticsearch responds at `http://localhost:9200`. If Prometheus has no application series, check the targets page and each Actuator endpoint. If a target is down, verify that its container is running and shares the Compose network.

## Kubernetes on K3d

Kubernetes uses the same tools with a different log collection mechanism. Filebeat runs as a DaemonSet and harvests node container logs from `/var/log/containers`; it adds Kubernetes metadata before sending events to Elasticsearch. Prometheus discovers pods in `hashimoto-core` with container ports `8080` or `8081`. Grafana's Prometheus data source points to the in-cluster Prometheus Service.

Deploy the monitoring and logging stacks from the repository root after the application and infrastructure are ready:

```bash
make deploy-ops
make deploy-logs
```

The services are cluster-internal. Use port-forwarding to open their UIs from your host:

```bash
kubectl port-forward -n hashimoto-logs svc/kibana-service 5601:5601
kubectl port-forward -n hashimoto-logs svc/elasticsearch-service 9200:9200
kubectl port-forward -n hashimoto-ops svc/prometheus-service 9090:9090
kubectl port-forward -n hashimoto-ops svc/grafana-service 3000:3000
```

Run each command in a separate terminal, then use the same URLs as the Compose table. Create a Kibana data view named `hashimoto-platform-logs-*` with `@timestamp` as its time field; the Kubernetes Filebeat output uses that daily index pattern. In Prometheus, check **Status → Targets** and query `up`. In Grafana, the `Prometheus` data source and **Hashimoto Platform Overview** dashboard are provisioned automatically. The dashboard JSON is kept in [`grafana/provisioning/dashboards/platform-overview.json`](../grafana/provisioning/dashboards/platform-overview.json); `make deploy-ops` packages it into a ConfigMap for Kubernetes. Alert rules and notification channels are not configured yet.

Useful Kubernetes diagnostics:

```bash
kubectl get pods -n hashimoto-logs
kubectl logs -n hashimoto-logs daemonset/filebeat --tail=100
kubectl logs -n hashimoto-logs statefulset/elasticsearch --tail=100
kubectl logs -n hashimoto-ops deployment/prometheus --tail=100
kubectl logs -n hashimoto-ops deployment/grafana --tail=100
```

The Kubernetes guide describes the namespace and storage layout in [Kubernetes architecture](k8s-architecture.md); installation and deployment order are in [K3d onboarding](k8s-onboarding.md).

## Configuration map

| Concern | Compose / hybrid file | Kubernetes file |
| --- | --- | --- |
| Filebeat inputs and Elasticsearch output | [`filebeat.yml`](../filebeat.yml) | [`filebeat-config.yaml`](../k8s/monitoring/logging/filebeat-config.yaml) |
| Prometheus scrape configuration | [`prometheus.yml`](../prometheus.yml) | [`prometheus-config.yaml`](../k8s/monitoring/prometheus-config.yaml) |
| Grafana data source and dashboard | [`grafana/provisioning/`](../grafana/provisioning/) | [`grafana/kustomization.yaml`](../grafana/kustomization.yaml) and [`grafana.yaml`](../k8s/monitoring/grafana.yaml) |
| Stack deployment | [`docker-compose.yaml`](../docker-compose.yaml) | [`k8s/monitoring/`](../k8s/monitoring/) |

Compose stores Elasticsearch and Grafana data in named Docker volumes. Kubernetes stores Elasticsearch and Prometheus/Grafana data in PVCs. Removing those volumes or PVCs deletes their retained local data. Elasticsearch security is disabled in the development manifests; do not expose these endpoints beyond a trusted development machine/network.

## Operational metrics versus customer charts

Prometheus and Grafana are for developers and operators. Their metrics describe the platform—availability, request volume, latency, and resource use. They must not become a store or query interface for an individual's symptoms, medication, meals, sleep, or activity.

Customer-facing charts belong in the Hashimoto application. When those product workflows are designed, domain APIs should return an authenticated user's own records or precomputed summaries; the frontend should render those responses. A future analytics/read-model component may calculate time-series summaries from approved domain events, then expose them through a user-scoped Insights API and the API Gateway. Authorization, consent, data provenance, time range, missing-data context, and clear non-diagnostic wording belong in that product flow. See the [target architecture's analytics flow](fullArchitectureHashimoto%20.md#future-real-time-analytics-and-ai-flow) and the roadmap's [first personal summaries milestone](../RoadMap.md#month-by-month-outline).
