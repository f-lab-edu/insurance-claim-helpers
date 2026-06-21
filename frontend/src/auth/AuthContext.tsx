import { createContext, useEffect, useState, type ReactNode } from 'react';
import { apiGet, apiPost } from '../api/client';

export interface User {
  id: string;
  email: string;
  createdAt: string;
}

export interface AuthContextValue {
  user: User | null;       // 로그인 사용자(null이면 비로그인)
  loading: boolean;        // 최초 /api/users/me 응답 전까지 true
  logout: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  // 앱 진입 시 1회: 현재 세션이 로그인 상태인지 확인한다.
  useEffect(() => {
    apiGet<User>('/api/users/me', { allow401: true })
      .then((me) => setUser(me))
      .catch(() => setUser(null))
      .finally(() => setLoading(false));
  }, []);

  // 로그아웃
  async function logout() {
    await apiPost<void>('/auth/logout');
    setUser(null);
  }

  return (
    <AuthContext.Provider value={{ user, loading, logout }}>
      {children}
    </AuthContext.Provider>
  );
}