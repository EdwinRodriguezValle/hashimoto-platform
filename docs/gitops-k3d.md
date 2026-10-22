# GitOps with Argo CD on local K3d

This guide connects the two Hashimoto repositories and deploys the platform to a K3d cluster on your own computer. GitHub Actions runs temporary build jobs and GHCR stores the container images; neither requires you to operate an always-on application server. Argo CD must be running in K3d and your computer must be on for reconciliation to happen.

## Repository responsibilities

- [`hashimoto-platform`](https://github.com/EdwinRodriguezValle/hashimoto-platform) contains the application source, Dockerfiles, Kubernetes resources under `k8s/`, and the build workflows.
- [`hashimoto-manifests`](https://github.com/EdwinRodriguezValle/hashimoto-manifests) contains Argo CD bootstrap settings and `Application` resources that connect Argo CD to the application repository.
- K3d is the local Kubernetes cluster. Argo CD pulls desired configuration from Git; GitHub Actions does not connect inbound to your computer.

The current application `Application` points to branch `main` and path `k8s`. The root `k8s/kustomization.yaml` composes the namespaces, infrastructure, workloads, and monitoring resources.

## Delivery flow

```text
feature branch → pull request → CI and security checks → merge to main
    → GitHub Actions builds SHA-tagged images and pushes them to GHCR
    → workflow updates the image tags in k8s/core/kustomization.yaml
    → Argo CD detects the Git commit and syncs the local K3d cluster
```

The feature branch and pull request provide review. The image build and deployment update happen after a change reaches `main`. The image tag is the source commit SHA, so you can identify and roll back a deployed version through Git.

## One-time local setup

### 1. Clone both repositories

```bash
git clone https://github.com/EdwinRodriguezValle/hashimoto-platform.git
git clone https://github.com/EdwinRodriguezValle/hashimoto-manifests.git
```

### 2. Create or start K3d

In `hashimoto-platform`, follow [K3d onboarding](k8s-onboarding.md) through the cluster creation step:

```bash
cd hashimoto-platform
make cluster-create
kubectl config current-context
kubectl get nodes
```

The expected context is `k3d-hashimoto-cluster`. If the cluster already exists, use `make cluster-use` instead of creating it again.

### 3. Install Argo CD

In `hashimoto-manifests`, install Argo CD into its own namespace. For a repeatable team setup, choose and document a specific Argo CD release instead of relying indefinitely on the moving `stable` URL.

```bash
cd ../hashimoto-manifests
kubectl create namespace argocd
kubectl apply --server-side --force-conflicts -n argocd \
  -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
kubectl apply -f argocd/bootstrap/
kubectl wait --for=condition=Available deployment/argocd-server \
  -n argocd --timeout=300s
```

Add this host entry on the computer running the browser and K3d:

```text
127.0.0.1 argocd.local
```

Then open [http://argocd.local:8085](http://argocd.local:8085). This bootstrap serves Argo CD over local HTTP. Keep it bound to your development machine; do not expose it to the public internet. Retrieve the initial admin password from the `argocd-initial-admin-secret` in the `argocd` namespace and change it after the first login.

### 4. Register the Applications

The root Application creates the platform namespaces. The workload Application reads the Kustomize root from `hashimoto-platform`. Apply both definitions:

```bash
kubectl apply -f argocd/applications/hashimoto-local.yaml
kubectl apply -f argocd/applications/api-gateway.yaml
kubectl get applications -n argocd
```

The workload Application currently has the display name `hashimoto-api-gateway`, but its `path: k8s` deploys the full set of resources included by the root Kustomization. Verify that its `repoURL`, `targetRevision: main`, and `path: k8s` match the repository and branch you intend to deploy.

### 5. Allow K3d to pull the images

The build workflow publishes images to GHCR. For a simple personal learning cluster, make the three GHCR packages public. If you keep them private, create a Kubernetes image pull secret in the workload namespaces and reference it from the pod specs; do not commit the secret value to Git.

The first healthy deployment requires image tags in `k8s/core/kustomization.yaml` to refer to images that already exist in GHCR. Merge a change to `main` after enabling the workflow, then check the Actions run and the resulting image-tag commit before expecting Argo CD to become healthy.

## Day-to-day work

1. Create a `feature/...` branch and make a focused change.
2. Push it and open a pull request to `main`.
3. Wait for backend CI, frontend CI, and the security scan. Fix failures before merging.
4. After merge, confirm the image publish workflow succeeds and updates `k8s/core/kustomization.yaml`.
5. In Argo CD, confirm the application becomes `Synced` and `Healthy`.

Use Git as the deployment control. Once Argo CD manages the platform resources, do not also run `make deploy-all` or `kubectl apply` for those same resources; that creates two competing sources of desired state. The manual Makefile workflow remains useful before Argo CD is installed or when deliberately working without GitOps.

## Useful checks and troubleshooting

```bash
kubectl get applications -n argocd
kubectl describe application hashimoto-api-gateway -n argocd
kubectl get pods -A
kubectl get events -A --sort-by=.lastTimestamp
```

| Symptom | First things to check |
| --- | --- |
| Argo reports an invalid destination cluster | For the local cluster, use `https://kubernetes.default.svc`; verify the destination in the installed Application. |
| Argo cannot render the source | Check the repo URL, branch, and path. The `k8s` path needs `k8s/kustomization.yaml`. |
| Pods show `ImagePullBackOff` | Check the image name and SHA tag in Kustomize, confirm the workflow published that tag, and check GHCR visibility or pull credentials. |
| Argo stays on an old commit | Check `targetRevision`, the latest commit on that branch, and the Application refresh/sync status. |
| A manual change gets reverted | Argo CD is enforcing Git state. Make the intended change in Git and let it sync. |

To roll back, revert the commit that changed the image tag or Kubernetes configuration and merge the revert. Argo CD then reconciles the previous desired state.

## Local-only security boundary

The current Kubernetes environment uses development configuration and is not a production deployment. Never commit real credentials, tokens, or personal health data. Before sharing the cluster or promoting this design to a hosted environment, move secrets to an appropriate secret-management solution, add resource limits and policies, and configure HTTPS and access control for Argo CD.
