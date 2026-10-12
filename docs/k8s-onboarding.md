# Incorporación: levantar Hashimoto en K3d


Sigue esta receta desde una máquina local nueva para crear el clúster, construir las imágenes y desplegar la plataforma. La ruta con comandos completos está dirigida a **Ubuntu 24.04 LTS de 64 bits (amd64 o arm64)**. Portainer puede permanecer instalado; no se necesita Rancher Manager. K3d crea nodos K3s como contenedores Docker y K3s incluye Traefik, que es el Ingress usado por los manifiestos.

Recomendación para ejecutar toda la pila, incluida la observabilidad: 4 CPU, 12 GiB de RAM y 60 GiB libres. Los puertos locales `8085` y `8443` deben estar disponibles. Si Docker, `kubectl`, `k3d`, Make y Git ya están instalados, salta al paso 2.

## 1. Instalar las herramientas (Ubuntu 24.04)

Instala utilidades requeridas y Docker Engine desde el repositorio oficial de Docker:

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl git make gnupg
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
sudo tee /etc/apt/sources.list.d/docker.sources <<EOF
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}")
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
EOF
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo systemctl enable --now docker
```

Permite que tu usuario ejecute Docker sin anteponer `sudo` (el grupo `docker` concede privilegios equivalentes a root) y **cierra sesión y vuelve a entrar** para que el cambio tenga efecto:

```bash
sudo groupadd --force docker
sudo usermod -aG docker "$USER"
```

Instala `kubectl` estable y valida su checksum. Estos comandos detectan la arquitectura amd64/arm64:

```bash
tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT
kubectl_arch="$(dpkg --print-architecture)"
kubectl_version="$(curl -fsSL https://dl.k8s.io/release/stable.txt)"
curl -fsSLo "$tmp_dir/kubectl" "https://dl.k8s.io/release/${kubectl_version}/bin/linux/${kubectl_arch}/kubectl"
curl -fsSLo "$tmp_dir/kubectl.sha256" "https://dl.k8s.io/release/${kubectl_version}/bin/linux/${kubectl_arch}/kubectl.sha256"
(cd "$tmp_dir" && echo "$(cat kubectl.sha256)  kubectl" | sha256sum --check)
sudo install -o root -g root -m 0755 "$tmp_dir/kubectl" /usr/local/bin/kubectl
```

Instala `k3d` desde el instalador oficial del proyecto:

```bash
curl -fsSL https://raw.githubusercontent.com/k3d-io/k3d/main/install.sh | bash
```

Abre una terminal nueva y confirma que Docker no requiera `sudo` y que las herramientas estén disponibles:

```bash
docker run --rm hello-world
docker compose version
kubectl version --client
k3d version
make --version
git --version
```

Docker Engine, `kubectl` y k3d publican sus procedimientos oficiales de instalación; esta receta aplica los pasos de [Docker](https://docs.docker.com/engine/install/ubuntu/), [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl-linux/) y [k3d](https://github.com/k3d-io/k3d/blob/main/docs/index.md).

## 2. Descargar el proyecto y preparar el host

Clona el repositorio (necesitas acceso de GitHub) y entra a su raíz:

```bash
git clone https://github.com/EdwinRodriguezValle/hashimoto-platform.git
cd hashimoto-platform
```

Elastic 8.15 requiere que el kernel permita al menos 262144 memory maps. Deja el ajuste activo ahora y después de reiniciar Ubuntu:

```bash
echo 'vm.max_map_count=262144' | sudo tee /etc/sysctl.d/99-hashimoto-elasticsearch.conf
sudo sysctl --system
sysctl vm.max_map_count
```

Añade el hostname local de la aplicación:

```bash
grep -qE '(^|[[:space:]])hashimoto\.local([[:space:]]|$)' /etc/hosts || echo '127.0.0.1 hashimoto.local' | sudo tee -a /etc/hosts
```

## 3. Crear el clúster local

Comprueba que no hay un clúster previo con ese nombre y créalo con el target del Makefile:

```bash
k3d cluster list
make cluster-create
kubectl cluster-info
kubectl get nodes -o wide
```

`make cluster-create` crea `hashimoto-cluster` con dos agentes, deja Traefik habilitado, publica `8085 → 80` y `8443 → 443` en el load balancer y selecciona el contexto `k3d-hashimoto-cluster`. Si el clúster ya existe, **no lo recrees**: usa `make cluster-use`; si estaba detenido, ejecuta `make up`.

## 4. Construir e importar las imágenes de la aplicación

Desde la raíz del repositorio ejecuta:

```bash
make build-all
```

El target compila Patient Service, API Gateway y frontend y carga sus imágenes en los nodos K3d. Puede tardar la primera vez porque descarga imágenes base y dependencias Maven/npm. Los Deployments usan `imagePullPolicy: Never`, así que las imágenes deben estar importadas antes de desplegar la aplicación.

## 5. Desplegar la plataforma

Ejecuta las capas en este orden:

```bash
make deploy-infra
make deploy-core
make deploy-ops
make deploy-logs
```

`make deploy-infra` crea los namespaces, aplica primero `k8s/infra/platform-env.yaml`, despliega PostgreSQL y espera a que existan `patient_db` y `keycloak_db`; luego espera a Kafka, aplica el realm de Keycloak con Kustomize y espera a que el realm responda. `make deploy-core` aplica la configuración de `hashimoto-core` antes de los servicios y espera a que estén listos. Los últimos dos targets son observabilidad (Prometheus/Grafana y Elasticsearch/Kibana/Filebeat) y son opcionales para probar el flujo de negocio.

Para que un único comando construya las imágenes y despliegue las cuatro capas:

```bash
make deploy-all
```

El Makefile comprueba que el contexto activo sea `k3d-hashimoto-cluster` antes de importar imágenes o aplicar manifiestos, para evitar desplegar en otro clúster por error.

Los Secrets de Kubernetes contienen contraseñas solo para desarrollo (`123` y `admin`). No uses estos valores fuera de tu máquina local.

PostgreSQL crea `patient_db` como base inicial y el script de inicialización crea `keycloak_db`. Patient Service (incluido Axon) usa `patient_db`; Keycloak usa `keycloak_db`. Los scripts de inicialización solo se ejecutan cuando el PVC de PostgreSQL está vacío. Eliminar un clúster K3d puede eliminar sus datos persistentes.

Keycloak usa la definición compartida del realm y los emisores/URLs JWKS por entorno descritos en [Keycloak en Hashimoto Platform](keycloak.md). En un volumen nuevo, el arranque importa el realm; si se reutiliza una base persistente y el realm ya existe, Keycloak omite esa importación.

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

Confirma las entradas HTTP de frontend, API y Keycloak:

```bash
curl --fail --silent --show-error --output /dev/null --write-out 'frontend %{http_code}\n' http://hashimoto.local:8085/
curl --fail --silent --show-error --output /dev/null --write-out 'realm %{http_code}\n' http://hashimoto.local:8085/auth/realms/hashimoto-realm
curl --silent --output /dev/null --write-out 'API sin token %{http_code}\n' http://hashimoto.local:8085/api/patients
```

Frontend y realm deben devolver `200`; la API sin token debe devolver `401`. Después inicia sesión y verifica el flujo con [el usuario de desarrollo](keycloak-user-workflow.md).

Para investigar un pod:

```bash
kubectl describe pod -n hashimoto-core <nombre-del-pod>
kubectl logs -n hashimoto-core deployment/patient-service --tail=100
kubectl logs -n hashimoto-infra deployment/keycloak --tail=100
```

Abre `http://hashimoto.local:8085/` cuando los targets anteriores terminen correctamente. Inicia sesión con la cuenta administrativa de Keycloak para crear el usuario de desarrollo; sigue [el flujo de usuario y roles](keycloak-user-workflow.md) para preparar `patient-read` y `patient-write`.

## 7. Operación diaria

```bash
make up       # iniciar el clúster detenido
make down     # detenerlo sin borrarlo
kubectl get pods -A
kubectl logs -n hashimoto-core deployment/api-gateway -f --tail=100
```

Detener el clúster conserva su configuración y almacenamiento local. `k3d cluster delete hashimoto-cluster` elimina el clúster y puede hacer perder datos guardados en sus volúmenes locales.

## 8. Límites del entorno local

Esta receta levanta los manifiestos actuales con credenciales de desarrollo y almacenamiento local. No es una instalación de producción ni un respaldo: eliminar el clúster puede eliminar datos persistentes. Grafana recibe Prometheus como datasource automáticamente, pero no se incluyen dashboards específicos. En macOS/Windows usa Docker Desktop y las instrucciones oficiales de instalación de las herramientas; los comandos de instalación y configuración de `/etc/hosts` de esta guía son específicos de Ubuntu.
