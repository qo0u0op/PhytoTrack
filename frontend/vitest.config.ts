import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  test: {
    // happy-dom 提供 DOM；localStorage 以 vitest.setup.ts 補上
    environment: 'happy-dom',
    setupFiles: ['./vitest.setup.ts'],
  },
})