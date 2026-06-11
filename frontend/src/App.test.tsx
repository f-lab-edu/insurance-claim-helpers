import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import App from './App';

describe('App 라우팅', () => {
  it('"/" 경로에서 메인 화면이 렌더된다', () => {
    // MemoryRouter로 테스트 중 라우팅 경로를 "/"로 고정
    render(
      <MemoryRouter initialEntries={['/']}>
        <App />
      </MemoryRouter>,
    );
    // HomePage placeholder의 제목이 화면에 존재해야 함
    expect(screen.getByRole('heading', { name: '메인' })).toBeInTheDocument();
  });
});