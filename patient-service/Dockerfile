# --- Etapa 1: Compilación y Construcción ---
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copiamos el POM Padre y las estructuras específicas de Pacientes desde la raíz
COPY pom.xml ./
COPY patient-service/pom.xml ./patient-service/
COPY patient-service/src ./patient-service/src

# Compilamos dirigiéndonos específicamente al módulo de pacientes
RUN mvn -f patient-service/pom.xml clean package -DskipTests

# --- Etapa 2: Imagen de Ejecución Ultra Ligera y Segura ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Creamos el usuario seguro para no correr la aplicación como root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copiamos el archivo .jar generado apuntando a la subcarpeta del target de pacientes
COPY --from=builder /app/patient-service/target/patient-service-*.jar app.jar

# Exponemos el puerto de negocio de pacientes
EXPOSE 8080

# Comando definitivo de arranque de la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]