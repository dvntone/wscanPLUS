import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = fileURLToPath(new URL('.', import.meta.url));

/**
 * Vite config for desktop/ renderer.
 *
 * - Builds renderer to `dist/` (loaded by Electron via main.js).
 * - Preserves the existing CSP (`script-src 'self'`): no CDN sources, no inline scripts.
 * - Emits hashed bundles + source maps for dev only; prod build strips them.
 * - `base: './'` so the file:// scheme used by Electron resolves correctly.
 */
export default defineConfig(({ mode }) => ({
  root: __dirname,
  base: './',
  build: {
    outDir: 'dist',
    emptyOutDir: true,
    sourcemap: mode === 'development',
    rollupOptions: {
      input: resolve(__dirname, 'index.html'),
      output: {
        // Predictable filenames so the CSP whitelist (if tightened later) stays simple.
        entryFileNames: 'assets/[name]-[hash].js',
        chunkFileNames: 'assets/[name]-[hash].js',
        assetFileNames: 'assets/[name]-[hash][extname]',
      },
    },
    target: 'chrome120', // Electron 41 ships Chromium 134; chrome120 is a safe floor.
  },
  resolve: {
    alias: {
      '@theme': resolve(__dirname, '../web/packages/theme/src'),
      '@desktop': resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 5174,
    strictPort: true,
  },
  plugins: [react()],
}));
