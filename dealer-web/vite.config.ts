import { fileURLToPath, URL } from 'node:url'

import tailwindcss from '@tailwindcss/vite'
import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

function manualChunks(id: string): string | undefined {
  const normalizedId = id.split('\\').join('/')
  if (
    normalizedId.includes('/node_modules/echarts/') ||
    normalizedId.includes('/node_modules/zrender/')
  ) {
    return 'vendor-charts'
  }
  return undefined
}

export default defineConfig({
  plugins: [vue(), tailwindcss()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    host: '0.0.0.0',
    port: 8081,
    open: true,
  },
  build: {
    rollupOptions: {
      output: { manualChunks },
    },
  },
  test: {
    globals: true,
    environment: 'happy-dom',
    setupFiles: ['./tests/setup.ts'],
    include: ['tests/**/*.test.{ts,tsx}'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      include: ['src/**/*.{ts,vue}'],
      exclude: ['src/app/main.ts', 'src/App.vue', 'src/assets/**'],
      all: true,
      thresholds: {
        statements: 10,
        branches: 0,
        functions: 8,
        lines: 10,
      },
    },
  },
})
