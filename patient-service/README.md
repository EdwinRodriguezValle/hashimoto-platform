# 🔄 Arquitectura del Flujo de Datos: Axon Framework + Apache Kafka

Este microservicio (`patient-service`) implementa los patrones **CQRS** (Command Query Responsibility Segregation) y **Event Sourcing** utilizando **Axon Framework**, y se integra con **Apache Kafka** como Message Broker distribuido para la comunicación hacia el exterior de la plataforma.

A continuación se detalla el ciclo de vida completo de la información desde que una petición HTTP ingresa por la API hasta que los datos se consolidan en la Base de Datos.

---

## 🗺️ Diagrama del Flujo de Información

Cliente / Postman / React ]│▼ (HTTP POST /api/v1/patients)┌────────────────────────────────────────────────────────┐│ 1. API LAYER (PatientController)                       ││    - Recibe el DTO y construye un Command.             ││    - Envía el Command al CommandGateway de Axon.       │└─────────────────────────┬──────────────────────────────┘│▼ (Axon Command Bus)┌────────────────────────────────────────────────────────┐│ 2. CORE DOMAIN (PatientAggregate)                      ││    - Se carga el Aggregate en memoria.                 ││    - Valida las reglas de negocio en el @CommandHandler.││    - Si todo es correcto, aplica (Apply) un Event.     │└─────────────────────────┬──────────────────────────────┘│▼ (Axon Event Bus / JPA Event Store)┌────────────────────────────────────────────────────────┐│ 3. PERSISTENCIA DE EVENTOS                             ││    - El evento se guarda en la tabla de Postgres:      ││      domain_event_entry (Event Sourcing).            │└─────────────────────────┬──────────────────────────────┘│┌────────────────┴────────────────┐▼ (Axon Tracking Event Processor)  ▼ (Axon-Kafka Extension)┌───────────────────────────────────────┐  ┌────────────────────────────────────┐│ 4A. PROYECCIÓN LOCAL (Read Model)    │  │ 4B. TRANSMISIÓN EXTERNA            ││  - @EventHandler escucha el evento. │  │  - El KafkaPublisher de Axon toma  ││  - Actualiza la tabla JPA de lectura  │  │    el evento automáticamente.      ││    (patient_projection).            │  │  - Lo publica en el tópico de      ││  - Guarda el estado actual listo para │  │    Kafka correspondiente.          ││    ser consultado por los @Query.   │  │                                    │└───────────────────────────────────────┘  └─────────────────┬──────────────────┘│▼ (Broker de Mensajería)┌────────────────────────────────────┐│ 5. APACHE KAFKA                    ││  - Tópico: patient-events        ││  - Disponible para que OTROS       ││    microservicios lo consuman de   ││    forma asíncrona.                │└────────────────────────────────────┘

## 🛠️ Explicación Paso a Paso del Flujo

### Paso 1: Capa de API y Comandos (`api`, `dto`, `command`)
Cuando el cliente envía un formulario para registrar un paciente, el controlador de Spring recibe el `PatientRequestDTO`. El controlador **no llama a un servicio tradicional para hacer un `repository.save()`**. En su lugar, transforma el DTO en un objeto de tipo **Command** (ej. `CreatePatientCommand`) y lo envía al **CommandGateway** de Axon.
* *Código involucrado:* `PatientController` ➔ `CommandGateway.sendAndWait(command)`.

### Paso 2: El Agregado y las Reglas de Negocio (`aggregate`)
El **Command Bus** de Axon entrega el comando al Agregado (`PatientAggregate`). El método marcado con `@CommandHandler` intercepta el comando. Aquí ocurre la validación (ej. verificar que el DNI sea válido, que la edad sea coherente). Si la regla de negocio se cumple, el Agregado ejecuta el método `AggregateLifecycle.apply()` disparando un **Event** (ej. `PatientCreatedEvent`).

### Paso 3: Almacenamiento del Evento (PostgreSQL - Event Store)
Antes de que pase cualquier otra cosa, Axon intercepta el evento y lo guarda de forma obligatoria en la tabla **`domain_event_entry`** de PostgreSQL. Esto es **Event Sourcing**: la base de datos no guarda el "estado" del paciente, guarda la *historia* de lo que le ha pasado al paciente.

### Paso 4A: La Proyección de Lectura (CQRS - `query`)
En segundo plano, un procesador de eventos de Axon (*Tracking Event Processor*) toma el evento guardado y lo envía a la clase de proyección (`PatientProjectionHandler`). El método marcado con `@EventHandler` toma los datos del evento y, utilizando un Repositorio JPA tradicional, los inserta o actualiza en la tabla de lectura (ej. `patient_entity`). Esta tabla está optimizada para que cuando el API Gateway haga un `GET`, la respuesta sea instantánea a través del **QueryGateway**.

### Paso 4B y 5: La Conexión con Apache Kafka (`config`, `event`)
Aquí es donde entra la extensión **Axon-Kafka**. Tienes una clase de configuración (`AxonKafkaConfig`) que enlaza el Event Bus de Axon con un productor de Kafka.
* Cuando el `PatientCreatedEvent` se guarda en el Event Store de Postgres, la extensión de Kafka lo captura de forma automática, lo serializa (normalmente a JSON o Avro) y lo envía inmediatamente a **Apache Kafka** en el tópico configurado (ej. `patient-events`).
* **¿Por qué hacemos esto?** Porque `patient-service` no debe ser un monolito. Al enviar el evento a Kafka, permites que en el futuro el servicio de alertas, el servicio de correos o el servicio de analítica reaccionen a la creación del paciente de manera 100% asíncrona y desacoplada, sin afectar el rendimiento de tu base de datos principal.

---

## 🗂️ Resumen de Responsabilidades de las Carpetas

- **`dto` / `api`**: Reciben la interacción externa del usuario.
- **`command`**: Intenciones de cambio en el sistema ("Quiero crear un paciente").
- **`aggregate`**: El cerebro táctico. Valida el comando y decide si el evento ocurre.
- **`event`**: Hechos históricos inmutables ("El paciente ya fue creado"). Se guardan en Postgres y viajan a Kafka.
- **`query`**: Escucha los eventos para armar tablas optimizadas para búsquedas rápidas.
- **`config`**: Enlaza los cables internos de Axon con los tópicos externos de Kafka de tu `docker-compose`.