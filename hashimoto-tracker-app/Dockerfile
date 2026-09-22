# --- Etapa 1: Compilación y Construcción ---
FROM node:20-alpine AS builder
WORKDIR /app

# Copiamos los archivos de dependencias primero (Aprovecha el caché de capas de Docker)
COPY package*.json ./

# Instalamos las dependencias de Node. Usar npm ci si existe package-lock.json, si no existe hacemos npm install como fallback.
RUN if [ -f package-lock.json ]; then \
      npm ci; \
    else \
      npm install --legacy-peer-deps; \
    fi

# Copiamos todo el código fuente del frontend
COPY . .

# Compilamos el proyecto (esto genera la carpeta "dist" con HTML/CSS/JS planos)
RUN npm run build

# --- Etapa 2: Imagen de Ejecución Ultra Ligera y Segura (Nginx) ---
FROM nginx:1.25-alpine
WORKDIR /usr/share/nginx/html

# 🔐 SECOPS: Limpiamos los archivos por defecto de Nginx
RUN rm -rf ./*

# Copiamos los archivos estáticos compilados desde la etapa anterior
COPY --from=builder /app/dist .

# INYECTAMOS TU CONFIGURACIÓN CUSTOM DE ENRUTAMIENTO REACT
COPY hashimoto-tracker-app/nginx.conf /etc/nginx/conf.d/default.conf

# Modificamos los permisos de Nginx para que corra como usuario NO-ROOT (Estándar Edwin UID 1000)
RUN touch /var/run/nginx.pid && \
    chown -R 1000:1000 /var/run/nginx.pid /var/cache/nginx /var/log/nginx /etc/nginx

# Cambiamos al usuario no-root seguro
USER 1000:1000

# El puerto estándar de producción web para Nginx
EXPOSE 80

# Arrancamos Nginx en primer plano
ENTRYPOINT ["nginx", "-g", "daemon off;"]
