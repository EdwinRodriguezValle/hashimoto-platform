# Keycloak user and role workflow

This guide walks through the current development flow: register with Hashimoto, grant application permissions in Keycloak, sign in, and verify that the backend receives and validates the access token.

## What is stored, and where

- Keycloak stores users, credentials, groups, and role assignments in PostgreSQL database `keycloak_db`.
- Patient Service stores patient records in `patient_db`. Keycloak users are not copied into that database as application users.
- Public self-registration is enabled in this local development setup. New accounts do not receive business roles automatically.
- The versioned realm JSON defines clients, scopes, initial roles/groups, and allowed redirect URIs. It is not the mechanism for creating each person's account during day-to-day use.

In a company, accounts may be managed directly in Keycloak or federated from a corporate directory such as LDAP or Active Directory. The organization defines its own joiner, mover, and leaver process. For this learning environment, use the Keycloak Admin Console.

## 1. Start the platform and choose the right environment

Start Keycloak, API Gateway, Patient Service, and the frontend in the mode you intend to use. In the Kubernetes onboarding flow, apply `k8s/infra/platform-env.yaml` before deploying the infrastructure.

| Mode | Keycloak Admin Console | Hashimoto application |
| --- | --- | --- |
| Hybrid (local Java and Vite, Compose infrastructure) | `http://localhost:8082/admin/` | `http://localhost:5173/` |
| Full Docker Compose | `http://localhost:8082/admin/` | `http://localhost:5173/` |
| Local Kubernetes (K3d) | `http://localhost:8085/auth/admin/` | `http://localhost:8085/` |

The hybrid and Compose modes have separate PostgreSQL volumes, so their Keycloak users and role assignments are separate even though the console URL is the same. Kubernetes has its own database as well. Make sure you are using the console for the same mode as the application you opened.

For Kubernetes, use `localhost` in the browser. The browser treats this local origin as a secure context and enables Web Crypto, which the OIDC PKCE flow requires. Sign in to the Admin Console with the administrator account configured for that environment. Select the `hashimoto-realm` realm before managing application users.

## 2. Create the permission group

The source realm defines the business roles `patient-read` and `patient-write`. A fresh Keycloak database receives them during the initial realm import. If the realm existed before those roles were added to the JSON, startup import will not add them; create them once from **Realm roles** or perform a deliberate realm update import.

1. Open **Groups** and create a group named `patient-clerks`.
2. Open the group, go to **Role mapping**, choose **Assign role**, and assign `patient-read` and `patient-write`.

Assigning roles to a group makes it easier to grant the same permissions to multiple users. The realm JSON versions the role definitions; group membership and user assignments are stored in `keycloak_db`.

## 3. Register a user and grant access

1. Open Hashimoto and choose **Create account**. Keycloak serves the registration form. The password goes directly to the identity provider through Authorization Code with PKCE; it is not sent to the patient API.
2. Complete registration and return to Hashimoto. The identity is stored in `keycloak_db`; this does not create a patient record.
3. In the Admin Console, open **Groups**, select `patient-clerks`, and add the new user as a member. The group grants `patient-read` and `patient-write`.
4. Sign out and sign in again to obtain an access token with the updated role assignments.

To create an account administratively instead of using self-registration, open **Users** → **Create new user**, set the password under **Credentials**, and add the user to `patient-clerks`.

The frontend hides the patient creation form from users without `patient-write`. The API also checks the role on the server; hiding a form is not a substitute for backend authorization.

Do not enable Direct Access Grants for this flow. The `hashimoto-tracker-web` client is public and uses Authorization Code with PKCE; Standard flow is enabled and the password grant is disabled.

## 4. Sign in and verify the request

1. Open the application for the selected environment and sign in.
2. In the browser developer tools, open **Network**. Submit a fictitious patient and find the `POST /api/patients` request.
3. Confirm the request contains an `Authorization: Bearer …` header and returns a successful HTTP response. Do not share the header or include the token in tickets, screenshots, or logs.
4. In Keycloak, confirm that the user belongs to `patient-clerks`. Keycloak owns the identity and roles; Patient Service processes the patient record and its associated storage/events.

To inspect the token payload locally, decode the middle section of the access token only on your own machine. A signed JWT is encoded, not encrypted: do not publish it or use public online decoders for real data. The payload should include `iss`, `sub`, `preferred_username`, `aud` containing `api-gateway-client`, and `realm_access.roles` containing the group-inherited roles. The backend validates the token signature, issuer, and audience; it does not trust the frontend to decide permissions.

## 5. What authorization the application enforces

The Gateway requires a token with a valid signature, issuer, and audience on protected routes. It forwards the bearer header to Patient Service, which repeats those checks and maps roles from `realm_access.roles` to Spring authorities (for example, `patient-read` becomes `ROLE_PATIENT_READ`). The controller requires `PATIENT_READ` for `GET /api/patients` and `PATIENT_WRITE` for `POST /api/patients` using `@PreAuthorize`.

This is the distinction between authentication (who you are) and authorization (what you may do): a valid token can pass the Gateway, but Patient Service still requires the role for each operation. An authenticated user without the required role should receive `403 Forbidden` from the service. After granting roles, sign in again to obtain a token with the updated assignments; roles are carried in the access token.

Self-registration is enabled in the versioned realm for this local lab. For an existing Kubernetes realm, `make keycloak-enable-registration` updates registration because the initial import does not overwrite existing realms. Registration can also be enabled in **Realm settings** → **Login** → **User registration**. In Compose, use the Admin Console at `http://localhost:8082/admin/` if its database was initialized before registration was enabled. Editing the JSON does not update an existing database.

## 6. Persistence and realm imports

Users and changes made in the Admin Console are stored in `keycloak_db`, not in the realm JSON. Keycloak's startup import skips an existing realm; editing the shared JSON does not synchronize users or automatically update that realm. See [Keycloak's import/export documentation](https://www.keycloak.org/server/importExport).

To reset this exercise, delete the test user or group in the Admin Console. Do not delete the PostgreSQL volume as a routine cleanup step: it may contain other persistent data. If you add base roles to the realm JSON, remember that an existing realm will not receive them automatically from the startup import.

## Troubleshooting

- **Sign-in fails:** confirm the user is enabled, the password has no pending update requirement, and the application is using the `hashimoto-realm` realm.
- **HTTP 401:** the token may be missing or expired, the issuer may not match, or the token may lack the `api-gateway-client` audience. Check the web client's audience mapper, `API_AUDIENCE`, and Gateway/Patient Service logs.
- **Roles are missing from the token:** check the user's group membership and the roles assigned to the group. Inspect the web client's access token, not its ID token.
- **HTTP 403:** the token is valid, but the user lacks `patient-read` for GET or `patient-write` for POST. Review role assignments and sign in again for a refreshed token.
