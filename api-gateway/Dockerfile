# --- Etapa 1: Compilación y Construcción ---
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copiamos el POM Padre y las estructuras específicas del Gateway desde el nuevo contexto
COPY pom.xml ./
COPY api-gateway/pom.xml ./api-gateway/
COPY api-gateway/src ./api-gateway/src

# Compilamos dirigiéndonos específicamente al módulo hijo desde la raíz de compilación
RUN mvn -f api-gateway/pom.xml clean package -DskipTests

# --- Etapa 2: Imagen de Ejecución Ultra Ligera ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Creamos el usuario seguro para no correr la aplicación como root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copiamos el archivo .jar generado en la etapa anterior apuntando a la subcarpeta del target
COPY --from=builder /app/api-gateway/target/api-gateway-*.jar app.jar

# Exponemos el puerto corporativo del Gateway
EXPOSE 8081

# Comando definitivo de arranque de la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]