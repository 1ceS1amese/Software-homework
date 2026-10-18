import vue from '@vitejs/plugin-vue'
import ui from '@nuxt/ui/vite'
import tailwindcss from '@tailwindcss/vite'
import { resolve } from 'path'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  // 配置只由启动脚本注入；禁止构建时自动读取可能含私有参数的 .env 文件。
  envDir: false,
  plugins: [vue(), tailwindcss(), ui({
    colorMode: false,
    // 导航图标定义在路由 TS 中，也必须纳入扫描，避免运行时请求外部图标服务。
    icon: { clientBundle: { scan: { globInclude: ['src/**/*.{vue,ts}'] } } },
    ui: { colors: { primary: 'teal', neutral: 'slate' } },
  })],
  resolve: {
    alias: {
      '@': resolve(import.meta.dirname, 'src'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api/v1': {
        target: process.env.VITE_API_TARGET || 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api': {
        target: process.env.VITE_API_TARGET || 'http://localhost:8081',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
})
