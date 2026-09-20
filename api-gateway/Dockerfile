# --- Etapa 1: Compilación y Construcción ---
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# 1. Copiamos el POM Padre obligatoriamente en la raíz del WORKDIR
COPY pom.xml .

# 2. Copiamos la estructura completa del módulo del Gateway en su respectiva subcarpeta
COPY api-gateway/ ./api-gateway/

# 💡 CORRECCIÓN MAESTRA: Compilamos desde la raíz indicando específicamente el proyecto (-pl)
# Esto permite que Maven lea el POM Padre correctamente y resuelva las dependencias del Monorepo
RUN mvn clean package -pl api-gateway -DskipTests

# --- Etapa 2: Imagen de Ejecución Ultra Ligera y Segura ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Creamos el usuario seguro para cumplir con tu estándar de SecOps (no-root)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copiamos el archivo .jar generado apuntando al target del módulo hijo
COPY --from=builder /app/api-gateway/target/api-gateway-*.jar app.jar

# Exponemos el puerto corporativo del Gateway
EXPOSE 8081

# Comando definitivo de arranque de la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]