# Hashimoto Platform - Módulo de Observabilidad (Métricas)

Este pilar de la plataforma administra la telemetría, rendimiento de hardware y salud de las Máquinas Virtuales de Java (JVM) en tiempo real mediante un esquema unificado de recolección tipo *pull* (raspado) no invasivo.

## 🏗️ Arquitectura del Stack de Métricas

1. **Spring Boot Actuator & Micrometer:** Cada microservicio (`api-gateway` y `patient-service`) recopila métricas nativas y las expone en formato compatible con Prometheus a través del endpoint `/actuator/prometheus`.
2. **Prometheus Server (Puerto 9090):** Actúa como el motor de almacenamiento de series temporales. Realiza un raspado (scrape) planificado cada 15 segundos hacia la red interna de Docker.
3. **Grafana Labs (Puerto 3000):** Capa de analítica y visualización interactiva conectada de forma nativa a Prometheus como Datasource aprovisionado por código.

## 🔐 Configuración de Seguridad (SecOps)

Para garantizar un entorno de producción seguro y de privilegios limitados:
* El contenedor de Prometheus se ejecuta bajo el estándar de ID de usuario estricto `user: "1000:1000"`.
* La persistencia se realiza en el directorio local del host `./prometheus_data`. Los permisos del directorio deben cambiarse explícitamente en el host físico ejecutando:  
  `sudo chown -R 1000:1000 ./prometheus_data`
* Se excluyeron los endpoints críticos de telemetría de los filtros de bloqueo de OAuth2/Keycloak y del enrutamiento de errores en Java:
  `.requestMatchers("/actuator/health", "/actuator/prometheus", "/error").permitAll()`

## 🚀 Despliegue y Verificación en Local

1. Compile las aplicaciones del Monorepo actualizando el POM Padre:
   ```bash
   mvn clean package -DskipTests
   ```
2. Levante la infraestructura completa reconstruyendo las imágenes:
   ```bash
   docker compose up -d --build
   ```
3. Verifique el estado de los canales de raspado ingresando a: `http://localhost:9090/targets`. Ambos servicios (`api-gateway` y `patient-service`) deben figurar en estado **UP**.
4. Ingrese al entorno gráfico de visualización en `http://localhost:3000` (Credenciales por defecto configuradas en el entorno: `admin` / `admin`).

## 📊 Dashboards Oficiales de la Comunidad Incorporados

Para visualizar el rendimiento de la plataforma sin configuraciones manuales, importe los siguientes identificadores dentro de Grafana seleccionando el origen de datos pre-configurado:
* **JVM Dashboard (Micrometer):** ID `4701` (Uso de memoria Heap, comportamiento del Garbage Collector e hilos activos).
* **Spring Boot 3.x Statistics:** ID `19011` (Monitoreo de tráfico, peticiones HTTP por segundo y tasas de respuesta por microservicio).