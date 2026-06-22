import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, afterEach, vi } from 'vitest';
import App from './App';

describe('App 라우팅', () => {
  beforeEach(() => {
    // App이 AuthProvider를 포함해 진입 시 /api/users/me를 호출한다.
    // 비로그인(401)으로 모킹해 네트워크 없이 라우팅만 검증한다.
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({ ok: false, status: 401 }),
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('"/" 경로에서 메인 화면 제목이 렌더된다', async () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <App />
      </MemoryRouter>,
    );
    // 새 HomePage의 실제 제목으로 라우팅 생존만 최소 보장
    // (AuthProvider의 비동기 상태 변화 때문에 findBy로 대기)
    expect(
      await screen.findByRole('heading', { name: '보험 약관, 내 편에서 해석해드려요' }),
    ).toBeInTheDocument();
  });
});