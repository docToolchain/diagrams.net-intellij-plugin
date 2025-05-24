import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'node:path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  base: './',
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  build: {
    outDir: 'dist',
    emptyOutDir: true
  },
  define: {
    // Make build timestamp available as a global constant
    __BUILD_TIMESTAMP__: JSON.stringify(process.env.VITE_BUILD_TIMESTAMP || 'Development')
  }
}) 