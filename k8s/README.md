# Kubernetes en Hashimoto Platform

Esta carpeta contiene los manifiestos Kubernetes usados para ejecutar la plataforma localmente en K3d. Está organizada por responsabilidad: aplicación (`core`), dependencias (`infra`), métricas (`monitoring`) y registros (`monitoring/logging`).

## Guías

- [Arquitectura actual](../docs/k8s-architecture.md): componentes que sí están representados en los manifiestos, flujo de tráfico, namespaces y límites conocidos.
- [Incorporación de desarrolladores](../docs/k8s-onboarding.md): herramientas, creación del clúster, compilación, despliegue y comprobaciones desde cero.

## Estructura

```text
k8s/
├── namespaces.yaml
├── core/
│   ├── platform-env.yaml
│   ├── api-gateway/       # Gateway, Ingress y puente de nombre a Keycloak
│   ├── frontend/          # Configuración nginx, Deployment y Service
│   └── patient-service/   # Deployment y Service
├── infra/
│   ├── platform-env.yaml  # ConfigMap y Secret para la infraestructura
│   ├── postgres/          # PostgreSQL, volumen e inicialización SQL
│   ├── kafka/             # Broker KRaft y volumen
│   └── keycloak/          # IdP y configuración del realm
└── monitoring/
    ├── prometheus*.yaml
    ├── grafana.yaml
    └── logging/           # Elasticsearch, Kibana y Filebeat
```

## Comandos habituales

Desde la raíz del repositorio:

```bash
make help
make up
make build-all
make deploy-all
```

`make deploy-all` usa el contexto Kubernetes activo y aplica manifiestos; no instala K3d, kubectl ni un Ingress Controller. La guía de incorporación contiene el procedimiento completo y los problemas que deben revisarse para lograr un despliegue funcional.

Para ver el estado:

```bash
kubectl get nodes
kubectl get pods -A
kubectl get pvc -A
```
