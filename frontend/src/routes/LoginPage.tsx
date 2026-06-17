import Header from '../components/Header';
import styles from './LoginPage.module.css';

// 로그인 화면 — Google OAuth로 이동
export default function LoginPage() {
  function goToGoogle() {
    window.location.href = '/auth/google';
  }

  return (
    <>
      <Header />
      <main className={styles.container}>
        <h1 className={styles.title}>보험 약관 AI 상담 서비스</h1>
        <button className={styles.googleButton} onClick={goToGoogle}>
          Google로 로그인
        </button>
      </main>
    </>
  );
}