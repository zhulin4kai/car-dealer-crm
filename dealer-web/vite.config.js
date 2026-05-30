import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],

  server:{
    host:'0.0.0.0',
    port: 8081,
    open: true,
  },

  test: {
    globals: true,
    environment: 'happy-dom',
    setupFiles: ['./tests/setup.js'],
    include: ['tests/**/*.test.{js,ts}'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      include: ['src/**/*.{js,vue}'],
      exclude: ['src/main.js', 'src/App.vue', 'src/assets/**', 'src/components/HelloWorld.vue'],
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
