import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// https://vite.dev/config/
export default defineConfig(({ command }) => ({
  plugins: [
    vue(),
    AutoImport({
      dts: false,
      resolvers: [
        ElementPlusResolver(),
      ],
    }),
    Components({
      dts: false,
      resolvers: [
        ElementPlusResolver({
          importStyle: 'css',
        }),
      ],
    }),
    ...(command === 'serve' ? [vueDevTools()] : []),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules')) {
            if (id.includes('element-plus')) {
              return 'vendor-element-plus'
            }
            if (id.includes('@popperjs/core')) {
              return 'vendor-popper'
            }
            if (id.includes('vue-i18n')) {
              return 'vendor-i18n'
            }
            if (id.includes('vue-router')) {
              return 'vendor-router'
            }
            if (id.includes('pinia')) {
              return 'vendor-pinia'
            }
            if (id.includes('vue') || id.includes('@vue')) {
              return 'vendor-vue'
            }
            if (id.includes('axios')) {
              return 'vendor-axios'
            }
            if (id.includes('pinyin-pro')) {
              return 'vendor-pinyin'
            }
          }

          if (id.includes('/src/i18n.ts')) {
            return 'app-i18n'
          }

          if (id.includes('/src/components/FuzzyProductSelect.vue')) {
            return 'app-fuzzy-product-select'
          }
        }
      }
    }
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  }
}))
