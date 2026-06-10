import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

// Vite 설정: React 플러그인 + 백엔드 프록시 + Vitest(테스트) 설정 통합
export default defineConfig({
  plugins: [react()],
  server: {
    // 개발 서버(:5173)에서 백엔드(:8080)로 프록시 — CORS 없이 API 호출 가능
    proxy: {
      '/api': 'http://localhost:8080',
      '/auth': 'http://localhost:8080',
    },
  },
  test: {
    // 브라우저 DOM 환경 시뮬레이션
    environment: 'jsdom',
    // describe/it/expect 전역 사용 (import 불필요)
    globals: true,
    // 각 테스트 파일 실행 전 jest-dom 매처 로드
    setupFiles: './src/test/setup.ts',
  },
});