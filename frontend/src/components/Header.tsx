import { Link } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';
import styles from './Header.module.css';

// 로고 + 로그인 상태에 따른 로그인/로그아웃 분기
export default function Header() {
  const { user, loading, logout } = useAuth();

  return (
    <header className={styles.header}>
      {/* 로고 클릭 시 메인으로 이동 */}
      <Link to="/" className={styles.logo}>보험 청구 도우미</Link>

      <nav>
        {!loading && (
          user
            ? <button className={styles.action} onClick={logout}>로그아웃</button>
            : <Link to="/login" className={styles.action}>로그인</Link>
        )}
      </nav>
    </header>
  );
}