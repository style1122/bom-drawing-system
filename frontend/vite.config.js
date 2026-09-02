import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  server: {
    host:'0.0.0.0',
    port: 3000,
    proxy: {
      // 与生产一致：/api 直接代理到后端（后端 context-path 为 /，接口在 /api/**）
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
