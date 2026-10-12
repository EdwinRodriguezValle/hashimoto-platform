# Flujo de usuario y roles con Keycloak

Esta guía recorre el flujo de desarrollo actual: crear un usuario de prueba en Keycloak, añadirlo a un grupo, iniciar sesión en Hashimoto y comprobar que el backend recibe y valida su access token.

## Qué se guarda y dónde

- Keycloak guarda usuarios, credenciales, grupos y asignaciones de roles en PostgreSQL, en la base `keycloak_db`.
- `patient-service` guarda los datos de pacientes en `patient_db`. Los usuarios de Keycloak no se crean como filas de usuarios en esa base.
- El JSON del realm configura clientes, scopes, roles/grupos iniciales y URI permitidas. No es el mecanismo para dar de alta cotidianamente a cada persona.

En una organización, las cuentas pueden administrarse directamente en Keycloak o federarse desde un directorio corporativo como LDAP/Active Directory. El alta/baja y las políticas dependen del proceso de identidad de la empresa. Para este entorno de aprendizaje usaremos la consola de Keycloak.

## 1. Arrancar la plataforma

Elige el entorno que ya estés usando y asegúrate de que Keycloak, API Gateway, Patient Service y frontend estén activos. En la receta Kubernetes, aplica `k8s/infra/platform-env.yaml` antes de desplegar la infraestructura.

| Entorno | Consola de administración | Aplicación |
| --- | --- | --- |
| Local híbrido o Compose | `http://localhost:8082/admin/` | `http://localhost:5173/` (híbrido) o `http://localhost:5173/` (Compose) |
| Kubernetes local | `http://hashimoto.local:8085/auth/admin/` | `http://hashimoto.local:8085/` |

En Kubernetes, el nombre `hashimoto.local` debe apuntar a `127.0.0.1` en el archivo hosts del sistema. Inicia sesión en la consola con la cuenta administrativa configurada para tu entorno; cambia a `hashimoto-realm` desde el selector de realms antes de gestionar identidades de la aplicación.

## 2. Crear permisos para el ejemplo

El realm fuente declara los roles de negocio `patient-read` y `patient-write`. En una instalación nueva los crea la importación. Si el realm ya existía antes de añadirlos al JSON, `--import-realm` no los agregará: créalos una vez desde **Realm roles** o realiza una importación de actualización deliberada.

1. Abre **Groups** y crea el grupo `patient-clerks`.
2. Dentro del grupo, abre **Role mapping**, elige **Assign role** y asigna `patient-read` y `patient-write`.

Asignar roles al grupo facilita dar a sus miembros los mismos permisos sin configurar cada cuenta por separado. La definición de roles está versionada con el realm; la pertenencia de usuarios al grupo se guarda en `keycloak_db`.

## 3. Crear un usuario y añadirlo al grupo

1. Abre **Users** → **Create new user**.
2. Crea `developer.demo` con un correo de prueba, guarda y deja la cuenta habilitada.
3. En la pestaña **Credentials**, establece una contraseña de desarrollo y desactiva **Temporary** para que no exija cambiarla en el primer acceso. No reutilices contraseñas reales.
4. En **Groups**, añade el usuario al grupo `patient-clerks`.
5. Vuelve a la ficha del usuario y revisa **Role mapping** para confirmar que hereda los roles del grupo.

No habilites Direct Access Grants para este flujo. El cliente `hashimoto-tracker-web` es público y usa Authorization Code con PKCE; su configuración tiene el flujo estándar activado y el password grant desactivado.

## 4. Iniciar sesión y comprobar el recorrido

1. Abre la aplicación en la tabla anterior. Si hay una sesión anterior activa, cierra sesión primero.
2. Keycloak mostrará el login. Inicia sesión con `developer.demo` y la contraseña temporal de desarrollo.
3. Abre las herramientas del navegador, pestaña **Network**, y busca la petición `POST /api/patients` después de enviar un paciente ficticio desde el formulario.
4. Comprueba que la petición lleva `Authorization: Bearer …` y que la respuesta HTTP es satisfactoria. No compartas el header ni copies el token a tickets o capturas.
5. En la consola de Keycloak, abre el usuario de prueba y confirma que sigue en `patient-clerks`. La autenticación y los roles son de Keycloak; el registro del paciente se procesa en Patient Service y su almacenamiento/eventos.

Para inspeccionar el payload localmente, puedes copiar el access token solo en tu sesión local y decodificar la parte central del JWT. Un JWT firmado está codificado, no cifrado: no publiques el token ni uses decodificadores web externos para datos reales. Deben aparecer `iss`, `sub`, `preferred_username` y `realm_access.roles` (incluidos los roles heredados del grupo). El backend valida firma y issuer; no confía en que el cliente web decida los permisos.

## 5. Qué autorización hace hoy la aplicación

Gateway exige un token válido para las rutas protegidas. Patient Service valida el token y convierte los roles de `realm_access.roles` a autoridades de Spring (por ejemplo, `patient-read` pasa a `ROLE_PATIENT_READ`). El controlador aplica `PATIENT_READ` a `GET /api/patients` y `PATIENT_WRITE` a `POST /api/patients` mediante `@PreAuthorize`.

Es una diferencia entre autenticación (quién eres) y autorización (qué puedes hacer): cualquier usuario con token válido puede pasar Gateway, pero solo quien tenga el rol correspondiente puede ejecutar cada operación en Patient Service. Un usuario autenticado sin el rol debería recibir `403 Forbidden` del servicio. Tras asignar roles, inicia una sesión nueva para obtener un token con las asignaciones actualizadas; los roles van dentro del access token.

## 6. Datos persistentes e importación

Los usuarios y los cambios hechos desde la consola quedan en `keycloak_db`, no en el JSON del realm. La importación de inicio de Keycloak omite un realm que ya existe; editar el archivo compartido no sincroniza esos usuarios ni actualiza automáticamente el realm existente. La [documentación de importación de Keycloak](https://www.keycloak.org/server/importExport) describe este comportamiento.

Para reiniciar el ejercicio, elimina el usuario o grupo desde la consola. No elimines el volumen PostgreSQL como método habitual: también contiene otros datos persistentes. Si modificas el JSON del realm para declarar roles base, recuerda que un realm ya existente no los recibirá automáticamente por el import de arranque.

## Diagnóstico rápido

- **No puedes iniciar sesión:** confirma que el usuario está habilitado, que la contraseña no está pendiente de cambio y que elegiste el realm `hashimoto-realm`.
- **HTTP 401:** token ausente, vencido o issuer no coincidente. Revisa el issuer del entorno en `docs/keycloak.md` y los logs de Gateway/Patient Service.
- **No aparecen roles en el token:** revisa la asignación del grupo al usuario y de roles al grupo; asegúrate de inspeccionar el access token del cliente web.
- **HTTP 403:** el token es válido, pero le falta `patient-read` para GET o `patient-write` para POST; revisa asignación de roles y vuelve a iniciar sesión para obtener un token actualizado.

