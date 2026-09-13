# ============================
# 1. Build stage
# ============================
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

# Copiar pom y descargar dependencias (cache)
COPY pom.xml .
RUN mvn -q -e dependency:go-offline

# Copiar código fuente
COPY src ./src

# Compilar
RUN mvn -q -e clean package -DskipTests


# ============================
# 2. Run stage
# ============================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copiar el JAR desde el build stage
COPY --from=build /app/target/*.jar app.jar

# Exponer puerto
EXPOSE 8080

# Ejecutar
ENTRYPOINT ["java", "-jar", "app.jar"]
CMD []
