import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    // 開發環境代理（Proxy）：將 /api 開頭的請求轉送後端（Spring Boot :8080）
    // 這樣前端可避免跨來源（CORS）問題，且不需在程式內寫死後端位址。
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
