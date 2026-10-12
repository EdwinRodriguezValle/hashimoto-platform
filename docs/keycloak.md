# Keycloak en Hashimoto Platform

## Propósito

Keycloak proporciona inicio de sesión y emite los tokens de acceso que usan la aplicación web y los servicios protegidos. En el navegador, la aplicación usa OpenID Connect Authorization Code con PKCE. API Gateway y Patient Service validan los JWT: verifican la firma con las claves públicas de Keycloak y comprueban que el `iss` coincida con el issuer esperado.

## Una definición del realm para todos los entornos

La fuente versionada del realm es [`k8s/infra/keycloak/hashimoto-realm-realm.json`](../k8s/infra/keycloak/hashimoto-realm-realm.json). Contiene el realm `hashimoto-realm`, clientes, scopes, los roles de negocio `patient-read`/`patient-write` y las URI permitidas. El cliente web permite explícitamente las direcciones de desarrollo para `localhost:5173`, `127.0.0.1:5173` y `hashimoto.local:8085`; no utiliza un origen comodín. Sus `rootUrl`, `adminUrl` y `baseUrl` quedan vacíos para que el mismo archivo no dependa de un host concreto.

El auto-registro está habilitado en el realm local. La pantalla inicial de Hashimoto ofrece **Iniciar sesión** y **Crear cuenta**; ambos flujos redirigen al formulario alojado por Keycloak mediante `keycloak-js` y Authorization Code con PKCE. La contraseña no pasa por el API de la plataforma. Keycloak guarda la identidad en `keycloak_db`; esto no crea automáticamente un perfil de paciente en `patient_db`.

Las cuentas auto-registradas no reciben los roles `patient-read` ni `patient-write`. Un administrador debe agregarlas al grupo `patient-clerks` (o conceder roles equivalentes) antes de usar las operaciones de pacientes. El formulario de escritura se oculta sin `patient-write`, y Patient Service vuelve a validar ese permiso en cada petición. Mantén el auto-registro limitado al entorno local; no habilites altas públicas para una instalación empresarial sin un flujo de aprobación, verificación de correo y política de acceso apropiados.

Compose monta ese JSON directamente en `/opt/keycloak/data/import/hashimoto-realm-realm.json`. Kubernetes genera el ConfigMap desde el mismo archivo mediante [`k8s/infra/keycloak/kustomization.yaml`](../k8s/infra/keycloak/kustomization.yaml), y el Deployment monta su contenido en el directorio de importación de Keycloak. El nombre del archivo sigue el formato `<realm>-realm.json` que Keycloak reconoce al importar desde ese directorio.

Al cambiar el JSON, Kustomize genera un ConfigMap con un sufijo derivado del contenido y actualiza la referencia del Deployment. Despliega los cambios con `make deploy-infra` o `kubectl apply -k k8s/infra/keycloak/`.

La importación de arranque es inicial: `--import-realm` omite un realm que ya existe en la base de datos. Un cambio en el archivo será usado por una instalación nueva; una base persistente requiere modificar el realm con la consola/API de administración o realizar un procedimiento de importación de actualización deliberado y con respaldo. Este comportamiento evita reemplazar estado existente durante cada reinicio ([documentación oficial de importación](https://www.keycloak.org/server/importExport)).

## Tres escenarios de desarrollo

El issuer es una URL pública que debe coincidir exactamente con el claim `iss` del token. La URL JWKS es la dirección desde la que cada proceso backend alcanza las claves públicas; puede variar con la red sin cambiar el issuer.

| Escenario | URL del navegador / issuer | JWKS usado por los backends |
| --- | --- | --- |
| Híbrido: servicios Java locales y dependencias en Compose | `http://localhost:8082/realms/hashimoto-realm` | `http://localhost:8082/realms/hashimoto-realm/protocol/openid-connect/certs` |
| Todo en Compose | `http://localhost:8082/realms/hashimoto-realm` | `http://keycloak:8080/realms/hashimoto-realm/protocol/openid-connect/certs` |
| Kubernetes local en K3d | `http://hashimoto.local:8085/auth/realms/hashimoto-realm` | `http://keycloak-service.hashimoto-infra.svc.cluster.local:8080/auth/realms/hashimoto-realm/protocol/openid-connect/certs` |

La web llama a Keycloak mediante la ruta relativa `/auth`. En modo IDE, Vite la reenvía a Keycloak en Compose; el Nginx de la imagen web hace el proxy en el escenario Compose completo; y en K3d Traefik enruta esa ruta al Ingress de Keycloak. Para Kubernetes, añade `127.0.0.1 hashimoto.local` a `/etc/hosts` y publica el puerto 8085 del balanceador K3d.

La configuración de hostname también cambia por entorno. Compose publica el servicio en `localhost:8082`. En Kubernetes, Keycloak sirve bajo `/auth`, publica `http://hashimoto.local:8085/auth` y confía en las cabeceras `X-Forwarded-*` del Ingress. La versión de la imagen actualmente fijada es Keycloak 24; al actualizarla se deben revisar los nombres y el comportamiento de las opciones de hostname/proxy para esa versión. La [guía oficial de proxy inverso](https://www.keycloak.org/server/reverseproxy) describe el manejo de las cabeceras reenviadas.

## Variables de backend

Los backends reciben dos propiedades separadas:

- `KEYCLOAK_ISSUER_URI`: issuer anunciado en los tokens y validado por la aplicación.
- `KEYCLOAK_JWK_SET_URI`: endpoint JWKS alcanzable desde el proceso que valida esos tokens.

La misma separación permite que un token tenga un issuer accesible para el navegador mientras un contenedor obtiene las claves mediante DNS interno de Compose o Kubernetes.

## Acceso administrativo y límites

Las credenciales administrativas provienen de variables de entorno en Compose y de un Secret de Kubernetes. Los valores incluidos en los manifiestos actuales son solo para desarrollo local: no deben reutilizarse ni exponerse en un entorno compartido. Keycloak usa PostgreSQL y la base `keycloak_db`, separada de `patient_db`.

Este despliegue usa `start-dev`, una réplica y configuración local sin TLS. Es apropiado para el aprendizaje y desarrollo en una máquina local, no es un diseño listo para producción.

Si el realm ya existía en la base persistente de Kubernetes, ejecuta `make keycloak-enable-registration` una vez (o habilita **Realm settings** → **Login** → **User registration** → **Save** desde la consola). En Compose, usa la consola. La importación inicial omite realms existentes, por lo que cambiar el JSON por sí solo no actualiza la base actual.

Para practicar el alta de un usuario, su pertenencia a grupos y la verificación del token en la API, sigue el [flujo de usuario y roles](keycloak-user-workflow.md).
