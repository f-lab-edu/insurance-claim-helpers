import { Link } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import styles from './Header.module.css';

// 로고 + 로그인 상태에 따른 로그인/로그아웃 분기
export default function Header() {
  const { user, loading, logout } = useAuth();

  return (
    <header className={styles.header}>
      {/* 로고: 방패 아이콘 + 서비스명. 클릭 시 메인으로 이동 */}
      <Link to="/" className={styles.logo}>
        <svg className={styles.logoIcon} width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
        </svg>
        보험약관 AI 상담
      </Link>

      <nav className={styles.nav}>
        {!loading && (
          user
            ? (
                <>
                  <Link to="/mypage" className={styles.account}>
                    <span className={styles.avatar}>
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                        <circle cx="12" cy="7" r="4" />
                      </svg>
                    </span>
                    <span className={styles.email}>{user.email}</span>
                  </Link>
                  <button className={styles.action} onClick={logout}>로그아웃</button>
                </>
              )
            : <Link to="/login" className={styles.action}>로그인</Link>
        )}
      </nav>
    </header>
  );
}