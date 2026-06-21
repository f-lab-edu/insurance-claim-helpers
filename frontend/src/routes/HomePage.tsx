import { useState, type ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiPost } from '../api/client';
import Header from '../components/Header';
import styles from './HomePage.module.css';

// 하단 서비스 소개 카드 3종
const features: { icon: ReactNode; title: string; desc: string }[] = [
  {
    // 문서 아이콘
    icon: (
      <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
        <polyline points="14 2 14 8 20 8" />
        <line x1="8" y1="13" x2="16" y2="13" />
        <line x1="8" y1="17" x2="16" y2="17" />
      </svg>
    ),
    title: '약관 업로드',
    desc: 'PDF 약관을 업로드하면 AI가 자동으로 분석합니다',
  },
  {
    // 말풍선 아이콘
    icon: (
      <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8z" />
      </svg>
    ),
    title: 'AI 상담',
    desc: '궁금한 점을 물어보면 약관 내용을 쉽게 설명해드립니다',
  },
  {
    // 방패 아이콘
    icon: (
      <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
      </svg>
    ),
    title: '청구 가능성 확인',
    desc: '보험금 청구 전 미리 가능 여부를 확인하세요',
  },
];

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

        <section className={styles.features}>
          {features.map((feature) => (
            <article key={feature.title} className={styles.card}>
              <span className={styles.cardIcon}>{feature.icon}</span>
              <h2 className={styles.cardTitle}>{feature.title}</h2>
              <p className={styles.cardDesc}>{feature.desc}</p>
            </article>
          ))}
        </section>
      </main>
    </>
  );
}