# -----------------------------------------------------------------------------
# HASHIMOTO PLATFORM - AUTOMATION MAKEFILE FOR KUBERNETES (K3d)
# -----------------------------------------------------------------------------

CLUSTER_NAME=hashimoto-cluster

.PHONY: help up down restart-ingress deploy-infra deploy-core deploy-ops deploy-logs build-patient build-gateway build-frontend build-all deploy-all

help: ## Muestra este menú de ayuda con los comandos disponibles
	@echo "Uso: make [comando]"
	@echo ""
	@echo "Comandos disponibles:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}'

# --- CONTROL DEL CLÚSTER LOCAL ---
up: ## Enciende el clúster de K3d y reactiva los nodos locales
	k3d cluster start $(CLUSTER_NAME)

down: ## Apaga el clúster de K3d de forma segura para liberar RAM
	k3d cluster stop $(CLUSTER_NAME)

restart-ingress: ## Reinicia en caliente el Ingress Controller de Nginx (Limpia caché)
	kubectl delete pod -n ingress-nginx --all

# --- COMPILACIÓN E INYECCIÓN DE IMÁGENES (DOCKER + K3D) ---
build-patient: ## Compila e importa la imagen local de patient-service
	docker build -t hashimoto/patient-service:latest -f patient-service/Dockerfile .
	k3d image import hashimoto/patient-service:latest -c $(CLUSTER_NAME)

build-gateway: ## Compila e importa la imagen local de api-gateway
	docker build -t hashimoto/api-gateway:latest -f api-gateway/Dockerfile .
	k3d image import hashimoto/api-gateway:latest -c $(CLUSTER_NAME)

build-frontend: ## Compila e importa la imagen local del frontend
	docker build -t hashimoto/frontend:latest -f hashimoto-tracker-app/Dockerfile ./hashimoto-tracker-app
	k3d image import hashimoto/frontend:latest -c $(CLUSTER_NAME)

build-all: build-patient build-gateway build-frontend ## Compila e importa las 3 imágenes de negocio juntas

# --- DESPLIEGUE POR CAPAS ORDENADAS ---
deploy-infra: ## Despliega Postgres, Kafka y Keycloak en hashimoto-infra
	kubectl apply -f k8s/namespaces.yaml
	kubectl apply -f k8s/infra/postgres/
	kubectl apply -f k8s/infra/kafka/
	kubectl apply -f k8s/infra/keycloak/

deploy-core: ## Despliega ConfigMaps, Ingress, Frontend y Microservicios en core
	kubectl apply -f k8s/core/platform-env.yaml
	kubectl apply -f k8s/core/patient-service/
	kubectl apply -f k8s/core/frontend/
	kubectl apply -f k8s/core/api-gateway/

deploy-ops: ## Despliega la telemetría (Prometheus y Grafana Enterprise) en ops
	kubectl apply -f k8s/monitoring/

deploy-logs: ## Despliega la pila de analítica de logs (ELK + Filebeat DaemonSet)
	kubectl apply -f k8s/monitoring/logging/

deploy-all: deploy-infra deploy-core deploy-ops deploy-logs ## Despliega las 4 capas completas del sistema
