# Incorporación: levantar Hashimoto en K3d

Esta guía reconstruye el flujo desde una máquina de desarrollo limpia. Está basada en los manifiestos y el Makefile actuales. El primer despliegue requiere corregir/verificar los puntos de arquitectura que se enumeran al final; por eso no se debe interpretar un `kubectl apply` exitoso como prueba de que la aplicación quedó lista.

## 1. Requisitos

Instala y comprueba estas herramientas en tu máquina:

- Docker Engine en ejecución
- `kubectl`
- `k3d`
- GNU Make
- Git

Clona el repositorio y ejecuta los comandos desde su raíz. Comprueba versiones e instalación:

```bash
docker version
kubectl version --client
k3d version
make --version
```

K3d usa K3s, una distribución ligera de Kubernetes del ecosistema Rancher. No es necesario instalar Rancher Manager para crear un clúster K3d. En una instalación K3s normal, Traefik viene habilitado; la configuración de Ingress de este repositorio declara la clase `traefik`.

## 2. Crear el clúster local

La siguiente receta coincide con los puertos que aparecen en la edición de `k8s/README.md`: el puerto 80 del load balancer del clúster se publica como 8085 del host, y 443 como 8443.

```bash
k3d cluster create hashimoto-cluster \
  --agents 2 \
  -p "8085:80@loadbalancer" \
  -p "8443:443@loadbalancer"
```

No desactives Traefik con esta receta. Si ya existe un clúster, no lo recrees: comprueba su configuración y contexto primero. Crear un clúster alternativo con `--disable=traefik` exige instalar un controlador compatible y alinear `ingressClassName` en los manifiestos.

```bash
k3d cluster list
kubectl config get-contexts
kubectl config use-context k3d-hashimoto-cluster
kubectl cluster-info
kubectl get nodes -o wide
```

La URL de entrada prevista por esta receta es `http://localhost:8085/`.

## 3. Crear namespaces y configuración

El orden importa: los recursos deben existir antes de que Deployments que los referencian arranquen.

```bash
kubectl apply -f k8s/namespaces.yaml
kubectl apply -f k8s/infra/platform-env.yaml
kubectl apply -f k8s/core/platform-env.yaml
```

Los Secrets actuales contienen contraseñas de desarrollo (`123` y `admin`) versionadas en YAML. No uses esos valores fuera de la máquina local. En un entorno compartido, reemplázalos por un mecanismo de secretos apropiado antes de aplicar.

PostgreSQL crea `patient_db` como base inicial y el script de inicialización crea `keycloak_db`. El Patient Service (incluido Axon) usa `patient_db`; Keycloak usa `keycloak_db`. PostgreSQL solo ejecuta esos scripts cuando el directorio de datos está vacío. Si reutilizas un volumen existente, cambiar `POSTGRES_DB` o el SQL no renombra ni crea bases automáticamente: haz una migración explícita o inicia con un volumen nuevo si no necesitas conservar sus datos.

## 4. Construir e importar imágenes

El clúster K3d no usa automáticamente las imágenes locales de Docker. El Makefile construye las tres imágenes y las importa a los nodos K3d:

```bash
make build-all
```

Este paso requiere que Docker esté disponible y que los Dockerfiles puedan compilar. Los Deployments usan `imagePullPolicy: Never`, así que si las imágenes no están importadas, los pods no arrancarán.

## 5. Desplegar capas

```bash
make deploy-infra
make deploy-core
make deploy-ops
make deploy-logs
```

`make deploy-infra` vuelve a aplicar namespaces y manifiestos de Postgres, Kafka y Keycloak. Se aplica explícitamente `infra/platform-env.yaml` en el paso anterior porque el target actual no lo hace. `deploy-core` aplica su ConfigMap/Secret y los servicios de negocio. Los targets de métricas y logging crean sus namespaces si el paso 3 ya se ejecutó.

El atajo es `make deploy-all`, pero en un primer despliegue conviene usar las capas por separado para localizar errores. `make deploy-all` no construye imágenes; ejecuta `make build-all` por separado.

## 6. Esperar y revisar el estado

```bash
kubectl get pods -A -w
```

En otra terminal, inspecciona cargas, volúmenes y entrada:

```bash
kubectl get deployments,statefulsets,daemonsets -A
kubectl get pods -A
kubectl get pvc -A
kubectl get ingress -A
kubectl get events -A --sort-by=.lastTimestamp
```

Para investigar un pod:

```bash
kubectl describe pod -n hashimoto-core <nombre-del-pod>
kubectl logs -n hashimoto-core deployment/patient-service --tail=100
kubectl logs -n hashimoto-infra deployment/keycloak --tail=100
```

Abre `http://localhost:8085/` cuando los pods de frontend y el controlador de Ingress estén listos. Para acceso directo temporal al frontend, usa:

```bash
kubectl port-forward -n hashimoto-core service/frontend-service 8085:80
```

## 7. Operación diaria

```bash
make up       # iniciar el clúster detenido
make down     # detenerlo sin borrarlo
kubectl get pods -A
kubectl logs -n hashimoto-core deployment/api-gateway -f --tail=100
```

Detener el clúster conserva su configuración y almacenamiento local. `k3d cluster delete hashimoto-cluster` elimina el clúster y puede hacer perder datos guardados en sus volúmenes locales.

## 8. Antes de declarar el entorno reproducible

Los manifiestos actuales tienen discrepancias que pueden impedir que todo quede listo:

- Debe aplicarse `k8s/infra/platform-env.yaml` antes de Postgres y Keycloak; el target de Makefile no lo aplica.
- Los Ingress usan clase Traefik, mientras que una receta antigua del README raíz desactiva Traefik e intenta descargar una definición Nginx desde una URL inválida/no versionada. Decide qué controlador usar y deja una única instalación reproducible.
- La ruta `/auth` tiene un backend DNS entre namespaces; el Service puente existente no está referenciado por el Ingress.
- Filebeat apunta a `http://cluster.local`, no al Service de Elasticsearch.
- `make deploy-ops` instala Prometheus y Grafana, pero no configura datasource/dashboard automáticamente.

La descripción y pasos para resolverlos están en [Arquitectura Kubernetes](k8s-architecture.md#observaciones-para-un-despliegue-reproducible). Hasta corregirlos, trata este procedimiento como mapa de instalación y aprendizaje, no como receta garantizada de clúster idéntico y operativo.
