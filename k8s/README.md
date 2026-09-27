Para mantener el aislamiento de recursos, la seguridad y el orden operacional, la plataforma se dividirá en tres Namespaces:
[ Namespace: hashimoto-core ] [ Namespace: hashimoto-infra ] [ Namespace: hashimoto-ops ]
├── api-gateway ├── postgres (StatefulSet) ├── prometheus & grafana
├── patient-service ├── kafka (KRaft mode) └── filebeat, elastic & kibana
└── frontend └── keycloak

 ****1. hashimoto-core:**** Aloja las aplicaciones de negocio, microservicios y la interfaz de usuario de la plataforma. 2. hashimoto
infra: Contiene los motores de datos, brokers de mensajería y el sistema de gestión de identidades y accesos (IAM). 3. hashimoto
ops: Agrupa las herramientas de observabilidad centralizada, recolección de métricas y almacenamiento de registros logs. 
 ****

****🗺️ 2.** Equivalencias Tecnológicas*** (De Docker a K8s)Servicio original en Docker Objeto de Kubernetes Estrategia de Persistencia y Redfrontend Deployment Stateless. Réplicas dinámicas exponiéndose al exterior vía Ingress.api-gateway Deployment Stateless. Expone las rutas internas a través de un Service unificado.patient-service Deployment Stateless. Inyecta configuraciones vía ConfigMaps y Secrets.postgres StatefulSet Requiere un PersistentVolumeClaim (PVC) dedicado para
resguardar la base de datos relacional.kafka StatefulSet Ejecución en modo KRaft (sin Zookeeper). Utiliza discos persistentes
e identificadores fijos por Pod.keycloak Deployment / StatefulSet Vinculado al servicio de PostgreSQL interno mediante DNS del
clúster.Pila ELK / Prometheus / Grafana Deployments / DaemonSets Monitorización de infraestructura y agregación de logs montando
rutas locales /app/logs. 

 ****🚀 3.** Fases de Ejecución del Plan**
El proceso se ejecutará de manera secuencial y controlada en las siguientes cuatro etapas:
**📅

_**Fase 1: **Abstracción de Entornos (ConfigMaps y Secrets)******_
Acción: Extraer todas las variables dinámicas (${DB_USER}, ${KAFKA_HOST}, etc.) del archivo .env.
Entregable: Archivos de configuración desacoplados en K8s para separar contraseñas de las configuraciones planas.

_💾 Fase 2: **Despliegue de la Capa de Datos e Infraestructura (hashimoto-infra)**_
Acción: Configurar el almacenamiento persistente (PVC) para asegurar que no ocurran pérdidas de información. Levantar
instancias estables de PostgreSQL, Kafka en modo KRaft y Keycloak con la importación automática del realm de seguridad.
Entregable: Servicios base saludables y accesibles mediante nombres DNS internos del clúster (ej. postgres
service.hashimoto-infra.svc.cluster.local).

_⚙️ Fase 3: **Despliegue de Aplicaciones de Negocio y Ruteo (hashimoto-core)_**
Acción: Adaptar los microservicios (patient-service y api-gateway) junto con el frontend para operar bajo
orquestación. Configurar políticas de reintentos, sondeos de salud (Liveness y Readiness Probes) y habilitar el tráfico
externo por medio de un Ingress Controller.
Entregable: Hashimoto Platform 100% operativa y accesible desde un navegador web apuntando al clúster.

_📊 Fase 4: **Telemetría y Observabilidad Centralizada (hashimoto-ops)**_
Acción: Traducir los servicios de Prometheus, Grafana y la pila Elastic (Elasticsearch, Filebeat, Kibana) a cargas de trabajo
nativas de Kubernetes para la correcta auditoría y monitoreo.
Entregable: Dashboards y visualizadores de logs recolectando datos en tiempo real de todos los pods del clúster.

_🛠️ 4. **Estructura Final de Archivos (/k8s)_**
Para mantener organizada tu carpeta existente en la raíz del proyecto, los manifiestos se ordenarán con prefijos numéricos que
dictan el orden lógico de aplicación en la consola (kubectl apply):

/k8s
k8s/
├── namespaces.yaml                # Define los namespaces (core, infra, monitoring, logging)
├── core/
│   ├── platform-env.yaml          # ConfigMap y Secrets compartidos por los microservicios
│   ├── api-gateway/
│   │   ├── deployment.yaml
│   │   ├── service.yaml
│   │   └── ingress.yaml           # El Ingress principal que recibe el tráfico del mundo exterior
│   ├── patient-service/
│   │   ├── deployment.yaml
│   │   └── service.yaml           # Se comunica internamente, no necesita Ingress propio
│   └── frontend/
│       ├── deployment.yaml
│       └── service.yaml
├── infra/
│   ├── postgres/
│   │   ├── pvc.yaml               # Almacenamiento persistente
│   │   ├── statefulset.yaml
│   │   └── service.yaml
│   ├── kafka/
│   │   ├── pvc.yaml               # Kafka con KRaft también requiere guardar sus logs
│   │   ├── statefulset.yaml       # Cambiado a StatefulSet en lugar de un yaml plano para resiliencia
│   │   └── service.yaml
│   └── keycloak/
│       ├── deployment.yaml
│       └── service.yaml
└── monitoring/                    # Pila de observabilidad y telemetría
├── prometheus.yaml
├── grafana.yaml
└── logging/                   # Subcarpeta para la pila ELK + Filebeat
├── elasticsearch.yaml
├── kibana.yaml
└── filebeat.yaml


📝 5. Próximos Pasos Técnicos Inmediatos
Generar el manifiesto base de Namespaces y configuraciones globales de entorno.
Construir la receta YAML de persistencia y el StatefulSet para PostgreSQL, replicando tus scripts de inicialización (./init
scripts).
Definir la infraestructura de red para intercomunicar la API Gateway y los microservicios sin colisiones.

kubectl apply -f k8s/namespaces.yaml
kubectl apply -f k8s/infra/postgres/
kubectl apply -f k8s/infra/kafka/

[Paso 1: Namespaces y Redes] ➔ [Paso 2: Base de Datos (Postgres)] ➔ [Paso 3: Identidad (Keycloak)] ➔ [Paso 4: Microservicios]

# Esto levanta el entorno de K8s completo, pero aún no expone la plataforma al exterior. Para ello, se requiere un Ingress Controller (Nginx, Traefik, etc.) y un DNS público apuntando a la IP del clúster.
¿Qué hace este comando? Crea un plano de control (Server), 2 nodos trabajadores (Agents) para distribuir tus microservicios, mapea el puerto 80 de tu laptop al clúster, y desactiva Traefik (el Ingress por defecto de K3s) para que podamos usar Nginx Ingress que es el que deseas.

```bash
k3d cluster create hashimoto-cluster \
--api-port 6550 \
-p "80:80@loadbalancer" \
-p "443:443@loadbalancer" \
--agents 2 \
--k3s-arg "--disable=traefik@server:*"
```

