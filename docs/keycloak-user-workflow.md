# Flujo de usuario y roles con Keycloak

Esta guía recorre el flujo de desarrollo actual: registrarse desde Hashimoto, asignar permisos de aplicación desde Keycloak, iniciar sesión y comprobar que el backend recibe y valida el access token.

## Qué se guarda y dónde

- Keycloak guarda usuarios, credenciales, grupos y asignaciones de roles en PostgreSQL, en la base `keycloak_db`.
- `patient-service` guarda los datos de pacientes en `patient_db`. Los usuarios de Keycloak no se crean como filas de usuarios en esa base.
- El registro público está habilitado para este entorno local. Las altas nuevas no reciben roles de negocio automáticamente.
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

## 3. Registrarse desde Hashimoto y conceder permisos

1. Abre Hashimoto y pulsa **Crear cuenta**. Keycloak sirve el formulario de registro; la contraseña se envía directamente al proveedor de identidad mediante Authorization Code con PKCE, no al API de pacientes.
2. Completa el formulario y vuelve a Hashimoto. La identidad queda guardada en `keycloak_db`; crear una cuenta no crea un paciente.
3. Un administrador abre **Groups**, selecciona `patient-clerks` y añade el nuevo usuario. Este grupo concede `patient-read` y `patient-write`.
4. Cierra y vuelve a iniciar sesión para recibir un token que contenga los permisos asignados.

Para crear una cuenta administrativamente en vez de usar el auto-registro, abre **Users** → **Create new user**, define la contraseña en **Credentials** y agrega al usuario al grupo `patient-clerks`.

El frontend oculta el formulario de alta de pacientes a quien no tiene `patient-write`. La API también comprueba el rol en el servidor; ocultar el formulario no sustituye esa autorización.

No habilites Direct Access Grants para este flujo. El cliente `hashimoto-tracker-web` es público y usa Authorization Code con PKCE; su configuración tiene el flujo estándar activado y el password grant desactivado.

## 4. Iniciar sesión y comprobar el recorrido

1. Abre la aplicación en la tabla anterior e inicia sesión.
2. Abre las herramientas del navegador, pestaña **Network**, y busca la petición `POST /api/patients` después de enviar un paciente ficticio desde el formulario.
3. Comprueba que la petición lleva `Authorization: Bearer …` y que la respuesta HTTP es satisfactoria. No compartas el header ni copies el token a tickets o capturas.
4. En la consola de Keycloak, abre el usuario y confirma que pertenece a `patient-clerks`. La identidad y los roles son de Keycloak; el registro del paciente se procesa en Patient Service y su almacenamiento/eventos.

Para inspeccionar el payload localmente, puedes copiar el access token solo en tu sesión local y decodificar la parte central del JWT. Un JWT firmado está codificado, no cifrado: no publiques el token ni uses decodificadores web externos para datos reales. Deben aparecer `iss`, `sub`, `preferred_username` y `realm_access.roles` (incluidos los roles heredados del grupo). El backend valida firma y issuer; no confía en que el cliente web decida los permisos.

## 5. Qué autorización hace hoy la aplicación

Gateway exige un token válido para las rutas protegidas. Patient Service valida el token y convierte los roles de `realm_access.roles` a autoridades de Spring (por ejemplo, `patient-read` pasa a `ROLE_PATIENT_READ`). El controlador aplica `PATIENT_READ` a `GET /api/patients` y `PATIENT_WRITE` a `POST /api/patients` mediante `@PreAuthorize`.

En el JSON versionado del realm, `registrationAllowed` está activado para el laboratorio local. En Kubernetes, `make keycloak-enable-registration` aplica el ajuste a una base que ya tenía el realm creado (la importación inicial no sobreescribe realms existentes). También se puede activar desde **Realm settings** → **Login** → **User registration**. En Compose, usa esa opción de la consola en `http://localhost:8082/admin/` si la base ya se había inicializado. La base de datos no se modifica al cambiar el JSON.

Es una diferencia entre autenticación (quién eres) y autorización (qué puedes hacer): cualquier usuario con token válido puede pasar Gateway, pero solo quien tenga el rol correspondiente puede ejecutar cada operación en Patient Service. Un usuario autenticado sin el rol debería recibir `403 Forbidden` del servicio. Tras asignar roles, inicia una sesión nueva para obtener un token con las asignaciones actualizadas; los roles van dentro del access token.

## 6. Datos persistentes e importación

Los usuarios y los cambios hechos desde la consola quedan en `keycloak_db`, no en el JSON del realm. La importación de inicio de Keycloak omite un realm que ya existe; editar el archivo compartido no sincroniza esos usuarios ni actualiza automáticamente el realm existente. La [documentación de importación de Keycloak](https://www.keycloak.org/server/importExport) describe este comportamiento.

Para reiniciar el ejercicio, elimina el usuario o grupo desde la consola. No elimines el volumen PostgreSQL como método habitual: también contiene otros datos persistentes. Si modificas el JSON del realm para declarar roles base, recuerda que un realm ya existente no los recibirá automáticamente por el import de arranque.

## Diagnóstico rápido

- **No puedes iniciar sesión:** confirma que el usuario está habilitado, que la contraseña no está pendiente de cambio y que elegiste el realm `hashimoto-realm`.
- **HTTP 401:** token ausente, vencido o issuer no coincidente. Revisa el issuer del entorno en `docs/keycloak.md` y los logs de Gateway/Patient Service.
- **No aparecen roles en el token:** revisa la asignación del grupo al usuario y de roles al grupo; asegúrate de inspeccionar el access token del cliente web.
- **HTTP 403:** el token es válido, pero le falta `patient-read` para GET o `patient-write` para POST; revisa asignación de roles y vuelve a iniciar sesión para obtener un token actualizado.

