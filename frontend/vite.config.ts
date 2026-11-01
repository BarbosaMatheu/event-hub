import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

// In development the API runs on :8080; proxying /api keeps the browser on a single origin
// (same as production behind nginx), so no CORS configuration is needed while developing.
// Pointing at another backend? Change the target below.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
  test: {
    environment: 'node',
    include: ['src/**/*.test.ts'],
  },
});
