import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// In dev, every /api/... call is forwarded to Spring Boot.
// The browser only talks to localhost:5173, so NO CORS setup is needed on the backend.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: "http://localhost:8081",
        changeOrigin: true,
      },
    },
  },
});
