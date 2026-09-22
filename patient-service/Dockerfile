# --- Etapa 1: Compilación y Construcción ---
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# 1. Copiamos el POM Padre obligatoriamente en la raíz del WORKDIR
COPY pom.xml ./

# 2. Copiamos la estructura completa (incluyendo el pom.xml padre)
COPY . .

# 💡 CORRECCIÓN MAESTRA: Compilamos desde la raíz indicando específicamente el archivo (-pl)
RUN mvn clean package -DskipTests -pl patient-service

# --- Etapa 2: Imagen de Ejecución Ultra Ligera y Segura ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Creamos el usuario seguro para cumplir con tu estándar de SecOps (no-root)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copiamos el archivo .jar generado apuntando al target del módulo
COPY --from=builder /app/patient-service/target/patient-service-*.jar app.jar

# Exponemos el puerto de negocio de pacientes
EXPOSE 8080

# Comando definitivo de arranque de la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]