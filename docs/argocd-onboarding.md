# Argo CD: primera reconciliación local

Esta primera integración hace que Argo CD observe la rama `feature/argocd-bootstrap` y gestione los recursos de aplicación en `k8s/core`. PostgreSQL, Kafka, Keycloak y observabilidad siguen instalándose con Makefile; se incorporarán a GitOps en fases posteriores.

## Antes de empezar

- El clúster local `k3d-hashimoto-cluster` debe estar activo.
- Ejecuta `make deploy-infra` para asegurar PostgreSQL, Kafka y Keycloak.
- Ejecuta `make build-all` para construir e importar las imágenes `hashimoto/*` en todos los nodos K3d. Sus etiquetas deben coincidir con `k8s/core/kustomization.yaml`.
- Publica `feature/argocd-bootstrap` en GitHub antes de crear la Application. El repositorio usa HTTPS de solo lectura y debe ser accesible para el clúster.

Argo CD solo reconcilia Kubernetes: no construye ni publica imágenes. Estas imágenes locales con `imagePullPolicy: Never` sirven para K3d; para otro clúster hay que publicarlas en un registry accesible y actualizar las referencias antes de sincronizar.

## 1. Instalar Argo CD

La versión está fijada a **v3.5.3** para que la instalación se pueda repetir. Los comandos usan el manifiesto oficial de instalación no-HA, adecuado para evaluación local, no producción.

```bash
kubectl config use-context k3d-hashimoto-cluster
kubectl create namespace argocd
kubectl apply --server-side --force-conflicts -n argocd \
  -f https://raw.githubusercontent.com/argoproj/argo-cd/v3.5.3/manifests/install.yaml
kubectl rollout status deployment/argocd-server -n argocd --timeout=300s
kubectl rollout status deployment/argocd-repo-server -n argocd --timeout=300s
kubectl rollout status statefulset/argocd-application-controller -n argocd --timeout=300s
```

La instalación oficial configura Argo CD con permisos para desplegar en el clúster donde corre. Mantén el API/UI en localhost durante esta prueba.

## 2. Registrar la aplicación

Desde la raíz del repositorio, aplica el manifiesto de bootstrap:

```bash
kubectl apply -f argocd/bootstrap/hashimoto-core.yaml
kubectl get applications -n argocd
kubectl describe application hashimoto-core -n argocd
```

La Application apunta a `feature/argocd-bootstrap` y a `k8s/core`. El auto-sync y self-heal hacen que Argo aplique cambios de Git y corrija drift en el clúster. `prune: false` evita borrar automáticamente objetos que desaparezcan del directorio mientras estás aprendiendo el flujo.

Comprueba la reconciliación:

```bash
kubectl get application hashimoto-core -n argocd -w
kubectl get pods -n hashimoto-core
```

El estado deseado es `Synced` y `Healthy`. La aplicación necesita acceso de red a GitHub para leer la rama.

## 3. Abrir la UI local

En un terminal:

```bash
kubectl port-forward svc/argocd-server -n argocd 8080:443
```

Abre `https://localhost:8080` y acepta el certificado local autofirmado. El usuario inicial es `admin`; obtén la contraseña inicial con:

```bash
kubectl -n argocd get secret argocd-initial-admin-secret \
  -o jsonpath='{.data.password}' | base64 --decode
```

Cambia esa contraseña después de iniciar sesión. No publiques el Secret ni la contraseña.

## Operación y límites

- Cambia el tag de imagen en `k8s/core/kustomization.yaml`, confirma el cambio y haz push a esta rama; Argo detectará y desplegará la actualización.
- `make deploy-core` sigue disponible para desarrollo manual, pero a partir del registro de la Application Argo CD es quien debe mantener esos recursos.
- La instalación y la Application se bootstrapean manualmente. El siguiente paso GitOps puede ser añadir una Application raíz para gestionar estas definiciones.
- Esta configuración es para K3d local. No uses el Secret de desarrollo, imágenes locales `Never` ni la instalación no-HA en un clúster compartido o de producción.

Referencias oficiales: [instalación de Argo CD](https://argo-cd.readthedocs.io/en/stable/operator-manual/installation/), [Kustomize en Argo CD](https://argo-cd.readthedocs.io/en/stable/user-guide/kustomize/) y [especificación de Applications](https://argo-cd.readthedocs.io/en/stable/operator-manual/declarative-setup/).
