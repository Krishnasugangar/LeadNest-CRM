import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

const apiTarget = process.env.VITE_PROXY_TARGET || 'http://localhost:8081';

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/api': apiTarget,
      '/v3': apiTarget,
      '/swagger-ui': apiTarget,
      '/swagger-ui.html': apiTarget,
    },
  },
});
