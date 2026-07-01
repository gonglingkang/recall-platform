import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    // 开发期如需直连后端，可启用下方代理替代 .env 中的 baseURL
    // proxy: {
    //   '/api': { target: 'http://localhost:20020', changeOrigin: true },
    // },
  },
})
