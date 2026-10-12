# -----------------------------------------------------------------------------
# HASHIMOTO PLATFORM - AUTOMATION MAKEFILE FOR KUBERNETES (K3d)
# -----------------------------------------------------------------------------

.NOTPARALLEL:

CLUSTER_NAME ?= hashimoto-cluster
K3D_AGENTS ?= 2

.PHONY: help cluster-create cluster-use check-context up down deploy-infra deploy-core deploy-ops deploy-logs keycloak-enable-registration build-patient build-gateway build-frontend build-all deploy-all

help: ## Muestra este menú de ayuda con los comandos disponibles
	@echo "Uso: make [comando]"
	@echo ""
	@echo "Comandos disponibles:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}'

# --- CONTROL DEL CLÚSTER LOCAL ---
cluster-create: ## Crea el clúster K3d local con Traefik y selecciona su contexto
	k3d cluster create $(CLUSTER_NAME) --agents $(K3D_AGENTS) -p "8085:80@loadbalancer" -p "8443:443@loadbalancer"
	kubectl config use-context k3d-$(CLUSTER_NAME)

cluster-use: ## Selecciona el contexto K3d de este proyecto
	kubectl config use-context k3d-$(CLUSTER_NAME)

check-context: ## Evita desplegar accidentalmente en otro clúster Kubernetes
	@expected="k3d-$(CLUSTER_NAME)"; current="$$(kubectl config current-context 2>/dev/null || true)"; \
	if [ "$$current" != "$$expected" ]; then \
		echo "Contexto actual: '$$current'. Ejecuta 'make cluster-use' (esperado: $$expected)."; \
		exit 1; \
	fi
	@echo "Contexto Kubernetes: k3d-$(CLUSTER_NAME)"

up: ## Enciende el clúster de K3d y reactiva los nodos locales
	k3d cluster start $(CLUSTER_NAME)
	kubectl config use-context k3d-$(CLUSTER_NAME)

down: ## Apaga el clúster de K3d de forma segura para liberar RAM
	k3d cluster stop $(CLUSTER_NAME)

# --- COMPILACIÓN E INYECCIÓN DE IMÁGENES (DOCKER + K3D) ---
build-patient: check-context ## Compila e importa la imagen local de patient-service
	docker build -t hashimoto/patient-service:latest -f patient-service/Dockerfile .
	k3d image import hashimoto/patient-service:latest -c $(CLUSTER_NAME)

build-gateway: check-context ## Compila e importa la imagen local de api-gateway
	docker build -t hashimoto/api-gateway:latest -f api-gateway/Dockerfile .
	k3d image import hashimoto/api-gateway:latest -c $(CLUSTER_NAME)

build-frontend: check-context ## Compila e importa la imagen local del frontend
	docker build -t hashimoto/frontend:latest -f hashimoto-tracker-app/Dockerfile ./hashimoto-tracker-app
	k3d image import hashimoto/frontend:latest -c $(CLUSTER_NAME)

build-all: build-patient build-gateway build-frontend ## Compila e importa las 3 imágenes de negocio juntas

# --- DESPLIEGUE POR CAPAS ORDENADAS ---
deploy-infra: check-context ## Configura y despliega Postgres, Kafka y Keycloak
	kubectl apply -f k8s/namespaces.yaml
	kubectl apply -f k8s/infra/platform-env.yaml
	kubectl apply -f k8s/infra/postgres/
	kubectl rollout status statefulset/postgres -n hashimoto-infra --timeout=300s
	kubectl apply -f k8s/infra/kafka/
	kubectl rollout status statefulset/kafka -n hashimoto-infra --timeout=300s
	kubectl apply -k k8s/infra/keycloak/
	kubectl rollout status deployment/keycloak -n hashimoto-infra --timeout=300s

keycloak-enable-registration: check-context ## Habilita el auto-registro local en un realm Keycloak ya inicializado
	kubectl exec -n hashimoto-infra deployment/keycloak -- sh -c 'set -e; trap '\''rm -f "$$HOME/.keycloak/kcadm.config"'\'' EXIT; /opt/keycloak/bin/kcadm.sh config credentials --server http://localhost:8080/auth --realm master --user "$$KEYCLOAK_ADMIN" --password "$$KEYCLOAK_ADMIN_PASSWORD"; /opt/keycloak/bin/kcadm.sh update realms/hashimoto-realm -s registrationAllowed=true; /opt/keycloak/bin/kcadm.sh update clients/86111e87-c561-49ed-ad4d-74dfc3851c63 -r hashimoto-realm -s '\''redirectUris=["http://localhost:8085/*","http://localhost:5173/*","http://127.0.0.1:5173/*"]'\'' -s '\''webOrigins=["http://localhost:8085","http://localhost:5173","http://127.0.0.1:5173"]'\'''

deploy-core: check-context ## Despliega ConfigMaps, Ingress, Frontend y microservicios
	kubectl apply -f k8s/core/platform-env.yaml
	kubectl apply -f k8s/core/patient-service/
	kubectl apply -f k8s/core/api-gateway/
	kubectl apply -f k8s/core/frontend/
	kubectl rollout restart deployment/patient-service -n hashimoto-core
	kubectl rollout restart deployment/api-gateway -n hashimoto-core
	kubectl rollout restart deployment/frontend -n hashimoto-core
	kubectl rollout status deployment/patient-service -n hashimoto-core --timeout=600s
	kubectl rollout status deployment/api-gateway -n hashimoto-core --timeout=300s
	kubectl rollout status deployment/frontend -n hashimoto-core --timeout=180s

deploy-ops: check-context ## Despliega Prometheus y Grafana en hashimoto-ops
	kubectl apply -f k8s/monitoring/prometheus-config.yaml
	kubectl apply -f k8s/monitoring/prometheus.yaml
	kubectl apply -f k8s/monitoring/grafana.yaml
	kubectl rollout status deployment/prometheus -n hashimoto-ops --timeout=300s
	kubectl rollout status deployment/grafana -n hashimoto-ops --timeout=300s

deploy-logs: check-context ## Despliega Elasticsearch, Kibana y Filebeat
	kubectl apply -f k8s/monitoring/logging/
	kubectl rollout status statefulset/elasticsearch -n hashimoto-logs --timeout=600s
	kubectl rollout status deployment/kibana -n hashimoto-logs --timeout=300s
	kubectl rollout status daemonset/filebeat -n hashimoto-logs --timeout=300s

deploy-all: check-context ## Construye imágenes y despliega la plataforma completa
	$(MAKE) build-all
	$(MAKE) deploy-infra
	$(MAKE) deploy-core
	$(MAKE) deploy-ops
	$(MAKE) deploy-logs
