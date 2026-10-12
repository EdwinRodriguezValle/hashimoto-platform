# Keycloak architecture and configuration

## Purpose

Keycloak is Hashimoto Platform's identity provider. It authenticates users and issues OpenID Connect access tokens. The browser application uses Authorization Code with PKCE. The API Gateway and Patient Service independently validate each bearer token's signature, issuer (`iss`), expiry, and audience (`aud`). The required API audience is `api-gateway-client`.

Keycloak stores identities and identity-management data. It does not store patient records: those belong to Patient Service in the separate `patient_db` database.

## Three ways to run the platform

The realm definition is the same in all three modes, but each mode runs its own Keycloak instance and PostgreSQL data. Users, passwords, groups, role assignments, and other changes made in one mode do not automatically appear in the other modes.

| Mode | What runs where | Browser app | Keycloak admin console | Public issuer in access tokens | JWKS URL used by backends |
| --- | --- | --- | --- | --- | --- |
| **Hybrid** | Frontend and Java services run on the host; infrastructure, including Keycloak, runs in Compose. | `http://localhost:5173/` | `http://localhost:8082/admin/` | `http://localhost:8082/realms/hashimoto-realm` | `http://localhost:8082/realms/hashimoto-realm/protocol/openid-connect/certs` |
| **Docker Compose** | Frontend, Java services, and infrastructure all run in Compose containers. | `http://localhost:5173/` | `http://localhost:8082/admin/` | `http://localhost:8082/realms/hashimoto-realm` | `http://keycloak:8080/realms/hashimoto-realm/protocol/openid-connect/certs` |
| **Kubernetes (K3d)** | Frontend, Java services, and infrastructure run in Kubernetes. | `http://localhost:8085/` | `http://localhost:8085/auth/admin/` | `http://localhost:8085/auth/realms/hashimoto-realm` | `http://keycloak-service.hashimoto-infra.svc.cluster.local:8080/auth/realms/hashimoto-realm/protocol/openid-connect/certs` |

The **issuer** is the public identity of the realm and must exactly match the `iss` claim in the token. The **JWKS URL** is only the network address a backend uses to download Keycloak's public signing keys. Docker Compose containers and Kubernetes pods use internal DNS for JWKS access; that does not change the issuer inside tokens.

### Hybrid mode

Start Keycloak and the other infrastructure dependencies with Docker Compose, then run the Java services and Vite on the host. The browser reaches Keycloak at `localhost:8082`; Vite forwards the frontend's relative `/auth` requests to that Compose Keycloak. Since the Java processes also run on the host, they use `localhost:8082` for both the issuer and JWKS. The Admin Console is `http://localhost:8082/admin/`.

### Full Docker Compose mode

All application processes run in Compose. Your browser still reaches Keycloak through the published host port at `localhost:8082`, and the frontend proxies `/auth` to the Keycloak container. The backend containers use the public issuer `localhost:8082` when validating tokens, but fetch signing keys from `keycloak:8080`, the Compose service DNS name. Open the Admin Console at `http://localhost:8082/admin/`.

### Kubernetes mode

Keycloak runs in `hashimoto-infra`; the frontend and APIs run in their application namespaces. Traefik publishes the platform through `localhost:8085`. The browser opens the app at `http://localhost:8085/` and uses the relative `/auth` route, which the Ingress sends to Keycloak. Use this `localhost` address in the browser: it is treated as a secure context for Web Crypto, which the OIDC PKCE flow requires. The browser-facing issuer is `http://localhost:8085/auth/realms/hashimoto-realm`. Backend pods use the Kubernetes Service DNS name to retrieve Keycloak's signing keys. The Admin Console is `http://localhost:8085/auth/admin/`.

The Kubernetes Keycloak deployment serves under `/auth`, publishes the external hostname `http://localhost:8085/auth`, and trusts the `X-Forwarded-*` headers from the Ingress. The Compose deployment publishes Keycloak directly on host port `8082` without the `/auth` path prefix.

## Shared realm definition

The version-controlled source is [`k8s/infra/keycloak/hashimoto-realm-realm.json`](../k8s/infra/keycloak/hashimoto-realm-realm.json). It defines the `hashimoto-realm` realm, its clients, scopes, API audience mapper, business roles (`patient-read` and `patient-write`), groups, and allowed development redirect URIs. The web client explicitly allows `localhost:5173`, `127.0.0.1:5173`, and `localhost:8085`; it does not use a wildcard origin. Its `rootUrl`, `adminUrl`, and `baseUrl` are empty so the file can be reused across these hostnames.

Compose mounts this file at `/opt/keycloak/data/import/hashimoto-realm-realm.json`. Kubernetes builds a ConfigMap from the same file through [`k8s/infra/keycloak/kustomization.yaml`](../k8s/infra/keycloak/kustomization.yaml) and mounts it in Keycloak's import directory. The filename follows Keycloak's `<realm>-realm.json` import convention.

On a new database, Keycloak imports the realm at startup. This is an initial import, not continuous synchronization: `--import-realm` skips a realm that already exists in the database. Editing the JSON therefore affects a fresh installation, but does not update a persistent realm or its users. For an existing database, update the realm through the Admin Console/API or perform a deliberate, backed-up update import. This prevents every restart from overwriting live realm state; see [Keycloak's import/export documentation](https://www.keycloak.org/server/importExport).

For Kubernetes, Kustomize creates a content-hashed ConfigMap and updates the Deployment reference when this JSON changes. Apply it with `make deploy-infra` or `kubectl apply -k k8s/infra/keycloak/`. This rolls out the mounted configuration, but does not make Keycloak re-import an existing realm.

## Clients, tokens, and API security

- **`hashimoto-tracker-web`** is the public browser client. It uses Authorization Code with PKCE and has no client secret. The browser never receives a confidential client credential.
- **`api-gateway-client`** represents the protected API audience. It is configured as a bearer-only client with no redirects or interactive login flow. It is not a secret that the Gateway uses to inspect tokens.
- The **`api audience` mapper** adds `api-gateway-client` to the web client's access token `aud` claim. The Gateway and Patient Service each require this audience along with a valid signature, issuer, and token lifetime. A token issued by the same realm for another application is not sufficient to call this API.

The Gateway acts as an OAuth2 Resource Server. It validates the incoming bearer token and forwards the original `Authorization` header to Patient Service; it does not exchange that token for another token. Patient Service validates the token again and enforces `patient-read` and `patient-write` permissions on each operation. The frontend may hide controls based on roles, but the backend is the authority for access decisions.

Both backends receive two separate settings:

- `KEYCLOAK_ISSUER_URI`: the public issuer that must match the token's `iss` claim.
- `KEYCLOAK_JWK_SET_URI`: the JWKS endpoint the backend can reach on its own network.

This separation allows the issuer to stay consistent with the browser-visible URL while a container retrieves signing keys through internal DNS. `API_AUDIENCE` is set to `api-gateway-client` in both backends and must match the audience mapper in Keycloak.

### Updating a persistent realm

If a realm was initialized before the audience mapper was added, importing the JSON again at startup will not update it. In the Admin Console, configure `api-gateway-client` as bearer-only (no Standard flow, redirects, web origins, or public-client setting). Then open **Clients** → `hashimoto-tracker-web` → **Client scopes** or **Mappers**, and add an **Audience** mapper for `api-gateway-client`. Add it to the access token, not the ID token. Sign in again to get a new access token and verify that its `aud` contains `api-gateway-client`. Finally rebuild and redeploy both backends if their audience settings changed.

## Registration, users, and roles

Self-registration is enabled for this local learning environment. The Hashimoto landing page's **Sign in** and **Create account** actions send the user to Keycloak, which handles credentials using OIDC Authorization Code with PKCE. The application API does not receive the user's password. A registered account is stored in `keycloak_db`; registration does not create a patient record in `patient_db`.

Newly registered users do not automatically receive `patient-read` or `patient-write`. An administrator must add them to the `patient-clerks` group (which grants those roles) or assign equivalent roles. Patient Service checks the relevant role for every patient operation. Keep public registration limited to local development; a shared or production environment needs an approved identity lifecycle and access policy.

For the complete user registration, role assignment, and API verification walkthrough, see the [Keycloak user and role workflow](keycloak-user-workflow.md).

## Admin access, persistence, and limits

Use the Admin Console URL in the environment table above. Sign in with that environment's configured administrator account, then select `hashimoto-realm` in the realm selector to manage application identities. The administrator username and password come from Docker Compose environment variables in Compose modes and a Kubernetes Secret in K3d; do not reuse the development values in a shared environment.

Each mode has its own PostgreSQL-backed Keycloak state. Keycloak stores users, credentials, groups, and role assignments in `keycloak_db`, separate from Patient Service's `patient_db`. Compose and Kubernetes do not share that database, so an account created in one mode is not automatically available in another.

The current setup pins Keycloak 24 and uses `start-dev`, one replica, local persistence, and no TLS. It is for local learning and development, not a production deployment. When upgrading Keycloak, review the hostname and proxy options for the target version against the [official reverse proxy guide](https://www.keycloak.org/server/reverseproxy).

If Kubernetes already has a persistent realm, run `make keycloak-enable-registration` once to enable self-registration on that existing realm, or enable **Realm settings** → **Login** → **User registration** in the Admin Console. In Compose, use the Admin Console if its database was initialized before registration was enabled. Editing the JSON alone does not update an existing realm.
