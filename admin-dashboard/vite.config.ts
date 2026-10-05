import { defineConfig, loadEnv } from 'vite';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const apiTarget = (env.VITE_API_BASE_URL || 'https://api.momentra.tech').replace(/\/$/, '');

  return {
    server: {
      port: 5180,
      strictPort: true,
      proxy: {
        '/admin/api': {
          target: apiTarget,
          changeOrigin: true,
          secure: true,
        },
      },
    },
    preview: {
      port: 5180,
      strictPort: true,
    },
  };
});
