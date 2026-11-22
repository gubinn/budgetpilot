import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { NaiveUiResolver } from 'unplugin-vue-components/resolvers'
import { resolve } from 'path'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      imports: [
        'vue',
        {
          'naive-ui': ['useDialog', 'useMessage', 'useNotification', 'useLoadingBar']
        }
      ]
    }),
    Components({
      resolvers: [NaiveUiResolver()]
    })
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:6060',
        changeOrigin: true
      }
    }
  },
  build: {
    rollupOptions: {
      output: {
        // 把体积最大的几个依赖拆成独立 chunk，避免首屏加载一个 1MB 的包
        manualChunks(id) {
          if (!id.includes('node_modules')) return
          if (id.includes('echarts') || id.includes('zrender')) return 'echarts'
          if (id.includes('@vicons')) return 'icons'
          if (id.includes('naive-ui') || id.includes('vueuc') || id.includes('treemate')
            || id.includes('vdirs') || id.includes('seemly') || id.includes('vooks')) return 'naive-ui'
          return 'vendor'
        }
      }
    }
  }
})
