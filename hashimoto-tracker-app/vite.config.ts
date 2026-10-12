import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      // Mantiene el navegador en el mismo origen durante el login local.
      '/auth': {
        target: 'http://localhost:8082',
        rewrite: (path) => path.replace(/^\/auth/, ''),
        changeOrigin: true,
        secure: false,
        xfwd: true,
      },
      // 👇 Intercepta las llamadas a '/api' y las redirige limpiamente a tu Gateway
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        secure: false,
      }
    }
  }
})
