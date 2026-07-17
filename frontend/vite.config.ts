import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        // Overridable because dev backends on this machine run on 8081
        // (port 8080 is occupied by an unrelated pre-existing httpd).
        target: process.env.IAMS_API_TARGET ?? 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
