import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiPost } from '../api/client';
import Header from '../components/Header';
import styles from './HomePage.module.css';

// 메인/랜딩 화면 — 서비스 소개 + "지금 시작하기" CTA.
export default function HomePage() {
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);

  // "지금 시작하기": 새 채팅 세션을 만들고 그 화면으로 이동
  // 인증 불필요 — 비로그인 사용자는 JSESSIONID가 session_key
  async function startChat() {
    setError(null);
    try {
      const session = await apiPost<{ id: string }>('/api/chat/sessions');
      navigate(`/chat/${session.id}`);
    } catch {
      setError('상담을 시작하지 못했습니다. 잠시 후 다시 시도해주세요.');
    }
  }

  return (
    <>
      <Header />
      <main className={styles.hero}>
        <h1 className={styles.title}>보험 약관, 내 편에서 해석해드려요</h1>
        <p className={styles.subtitle}>청구 가능 여부를 미리 확인하세요</p>
        <button className={styles.cta} onClick={startChat}>지금 시작하기</button>
        {error && <p className={styles.error}>{error}</p>}
      </main>
    </>
  );
}