# Onboarding: run Hashimoto on K3d

Follow this guide on a new local machine to create the cluster, build the images, and deploy the platform. The complete command sequence targets **Ubuntu 24.04 LTS, 64-bit (amd64 or arm64)**. Portainer can remain installed; Rancher Manager is not required. K3d runs K3s nodes as Docker containers. K3s includes Traefik, which is the Ingress controller used by these manifests.

Recommended host resources for the full stack, including observability: 4 CPUs, 12 GiB RAM, and 60 GiB free disk. Host ports `8085` and `8443` must be available. If Docker, `kubectl`, `k3d`, Make, and Git are already installed, skip to step 2.

## 1. Install the tools (Ubuntu 24.04)

Install the required utilities and Docker Engine from Docker's official repository:

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

Allow your user to run Docker without `sudo` (membership in the `docker` group grants root-equivalent privileges), then **log out and back in** for the group change to take effect:

```bash
sudo groupadd --force docker
sudo usermod -aG docker "$USER"
```

Install the latest stable `kubectl` and verify its checksum. These commands detect amd64/arm64:

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

Install `k3d` using the project's official installer:

```bash
curl -fsSL https://raw.githubusercontent.com/k3d-io/k3d/main/install.sh | bash
```

Open a new terminal and confirm Docker works without `sudo` and the tools are available:

```bash
docker run --rm hello-world
docker compose version
kubectl version --client
k3d version
make --version
git --version
```

Docker Engine, `kubectl`, and K3d publish their official installation procedures: [Docker](https://docs.docker.com/engine/install/ubuntu/), [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl-linux/), and [K3d](https://github.com/k3d-io/k3d/blob/main/docs/index.md).

## 2. Clone the project and prepare the host

Clone the repository (GitHub access is required) and enter the project root:

```bash
git clone https://github.com/EdwinRodriguezValle/hashimoto-platform.git
cd hashimoto-platform
```

### Configure the local `hashimoto.local` hostname

Add the hostname alias to the host's local name resolution so the machine can resolve `hashimoto.local` to its own loopback network address. On Ubuntu, edit `/etc/hosts`:

```bash
sudo nano /etc/hosts
```

Add this line, or update an existing entry for the same hostname:

```text
127.0.0.1 hashimoto.local hashimoto
```

Save the file and verify IPv4 name resolution:

```bash
getent ahostsv4 hashimoto.local
```

The result should include `127.0.0.1`. This host entry makes the alias resolve; it does not create the K3d cluster or publish its ports.

**For the current Kubernetes sign-in flow, open `http://localhost:8085/`, not `http://hashimoto.local:8085/`.** The app's OIDC/PKCE setup requires browser Web Crypto, which is available on the local `localhost` origin. The current Keycloak issuer and Ingress host are also configured for `localhost`; plain HTTP on `hashimoto.local` is not configured as a supported OIDC origin. The alias is included for local hostname resolution, but using it for the complete login flow requires a separate HTTPS and Keycloak hostname/redirect configuration.

Elasticsearch 8.15 requires the kernel to allow at least 262144 memory maps. Apply this setting now and keep it across Ubuntu restarts:

```bash
echo 'vm.max_map_count=262144' | sudo tee /etc/sysctl.d/99-hashimoto-elasticsearch.conf
sudo sysctl --system
sysctl vm.max_map_count
```

## 3. Create the local cluster

Check whether a cluster with this name already exists, then create it using the Makefile target:

```bash
k3d cluster list
make cluster-create
kubectl cluster-info
kubectl get nodes -o wide
```

`make cluster-create` creates `hashimoto-cluster` with two agents, keeps Traefik enabled, publishes `8085 → 80` and `8443 → 443` on the load balancer, and selects context `k3d-hashimoto-cluster`. If the cluster already exists, **do not recreate it**: use `make cluster-use`; if it is stopped, run `make up`.

## 4. Build and import the application images

From the repository root, run:

```bash
make build-all
```

This target builds Patient Service, API Gateway, and the frontend, then imports their images into the K3d nodes. The first run can take time because it downloads base images and Maven/npm dependencies. The Deployments use `imagePullPolicy: Never`, so images must be imported before deploying the application.

## 5. Deploy the platform

Apply the layers in this order:

```bash
make deploy-infra
make deploy-core
make deploy-ops
make deploy-logs
```

`make deploy-infra` creates the namespaces, applies `k8s/infra/platform-env.yaml` first, deploys PostgreSQL and waits for `patient_db` and `keycloak_db` to exist, then waits for Kafka, applies the Keycloak realm with Kustomize, and waits for the realm endpoint. `make deploy-core` applies `hashimoto-core` configuration before its services and waits for them to become ready. The final two targets deploy observability (Prometheus/Grafana and Elasticsearch/Kibana/Filebeat); they are optional for testing the business flow.

To build the images and deploy all four layers with one command:

```bash
make deploy-all
```

Before importing images or applying manifests, the Makefile checks that the active context is `k3d-hashimoto-cluster` to avoid deploying to another cluster by mistake.

Kubernetes Secrets contain development-only passwords (`123` and `admin`). Never use these values outside your local machine.

PostgreSQL creates `patient_db` as its initial database, and the initialization script creates `keycloak_db`. Patient Service, including Axon, uses `patient_db`; Keycloak uses `keycloak_db`. Initialization scripts run only when the PostgreSQL PVC is empty. Deleting a K3d cluster may delete its persistent data.

Keycloak uses the shared realm definition and environment-specific issuer/JWKS URLs described in [Keycloak architecture and configuration](keycloak.md). A new database imports the realm at startup. If the database is persistent and the realm already exists, Keycloak skips that import.

## 6. Wait for and inspect the deployment

Watch pods until they are ready:

```bash
kubectl get pods -A -w
```

In another terminal, inspect workloads, storage, Ingress, and recent events:

```bash
kubectl get deployments,statefulsets,daemonsets -A
kubectl get pods -A
kubectl get pvc -A
kubectl get ingress -A
kubectl get events -A --sort-by=.lastTimestamp
```

Check the frontend, Keycloak realm, and protected API entry points:

```bash
curl --fail --silent --show-error --output /dev/null --write-out 'frontend %{http_code}\n' http://localhost:8085/
curl --fail --silent --show-error --output /dev/null --write-out 'realm %{http_code}\n' http://localhost:8085/auth/realms/hashimoto-realm
curl --silent --output /dev/null --write-out 'API without token %{http_code}\n' http://localhost:8085/api/patients
```

The frontend and realm should return `200`; the API without a token should return `401`. Then sign in and follow the [Keycloak user and role workflow](keycloak-user-workflow.md).

To investigate a pod:

```bash
kubectl describe pod -n hashimoto-core <pod-name>
kubectl logs -n hashimoto-core deployment/patient-service --tail=100
kubectl logs -n hashimoto-infra deployment/keycloak --tail=100
```

Open `http://localhost:8085/` after the checks pass. Choose **Create account** to register; an administrator must then add the account to `patient-clerks` to grant patient permissions. For a realm that already existed, run `make keycloak-enable-registration` once to enable registration and update the client's allowed origin. See the [Keycloak user and role workflow](keycloak-user-workflow.md) for the full process.

## 7. Daily operations

```bash
make up       # start the stopped cluster
make down     # stop it without deleting it
kubectl get pods -A
kubectl logs -n hashimoto-core deployment/api-gateway -f --tail=100
```

Stopping the cluster preserves its configuration and local storage. `k3d cluster delete hashimoto-cluster` deletes the cluster and may destroy data stored in its local volumes.

## 8. Local environment limitations

This guide deploys the current manifests with development credentials and local storage. It is neither a production installation nor a backup: deleting the cluster may destroy persistent data. Grafana automatically receives Prometheus as a data source, but application dashboards are not included. On macOS or Windows, use Docker Desktop and the tools' official installation instructions; the package installation commands and `/etc/hosts` steps in this guide are specific to Ubuntu.
