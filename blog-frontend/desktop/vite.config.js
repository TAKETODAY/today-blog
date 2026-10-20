import { fileURLToPath, URL } from 'node:url'
import { mkdir, writeFile } from 'node:fs/promises'
import { dirname, resolve } from 'node:path'
import { gzipSync } from 'node:zlib'
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

function gzipAssets() {
  return {
    name: 'gzip-assets',
    apply: 'build',
    async writeBundle(options, bundle) {
      for (const [fileName, asset] of Object.entries(bundle)) {
        if (asset.type !== 'asset' && asset.type !== 'chunk') continue
        if (!/\.(js|css|svg)$/.test(fileName)) continue
        const source = asset.type === 'chunk' ? asset.code : asset.source
        if (Buffer.byteLength(source) < 1024) continue
        const gzipFile = resolve(options.dir, `${fileName}.gz`)
        await mkdir(dirname(gzipFile), { recursive: true })
        await writeFile(gzipFile, gzipSync(source))
      }
    },
  }
}

function minifyHtml() {
  return {
    name: 'minify-html',
    apply: 'build',
    transformIndexHtml(html) {
      return html
        .replace(/<!--(?!\[if)[\s\S]*?-->/g, '')
        .replace(/>\s+</g, '><')
        .trim()
    },
  }
}

export default defineConfig(({ mode }) => ({
  plugins: [react(), minifyHtml(), gzipAssets()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
      'src': fileURLToPath(new URL('./src', import.meta.url)),
      'core': fileURLToPath(new URL('../common', import.meta.url)),
    },
  },
  css: {
    preprocessorOptions: {
      less: {
        javascriptEnabled: true,
        modifyVars: {
          '@primary-color': '#337ab7',
        },
      },
    },
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'build',
    chunkSizeWarningLimit: 1000,
    rollupOptions: {
      output: {
        manualChunks: {
          react: ['react', 'react-dom', 'react-redux', 'react-router-dom'],
          antd: ['antd', '@ant-design/icons'],
          codemirror: [
            'codemirror',
            'codemirror-spell-checker',
          ],
          markdown: [
            'markdown-it',
            'markdown-it-emoji',
          ],
          highlight: [
            'highlight.js',
          ],
        },
      },
    },
    esbuild: {
      drop: mode === 'production' ? ['console', 'debugger'] : [],
    },
  },
  define: {
    'process.env.NODE_ENV': JSON.stringify(mode),
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/setupTests.js',
    css: true,
  },
}))
