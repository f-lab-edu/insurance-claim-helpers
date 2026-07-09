import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import Header from '../components/Header';
import Toast from '../components/Toast';
import ConfirmModal from '../components/ConfirmModal';
import { useAuth } from '../auth/useAuth';
import {
  deleteClaimCriteria,
  getMyChatSessions,
  getMyClaimCriteria,
  type ChatSessionListItem,
  type ClaimCriteriaListItem,
} from '../api/mypage';
import styles from './MyPage.module.css';

function formatFileSize(bytes: number): string {
  if(bytes < 1024) {
    return `${bytes} B`;
  }
  const kb = bytes / 1024;
  if(kb < 1024) {
    return `${kb.toFixed(1)} KB`;
  }
  return `${(kb / 1024).toFixed(1)} MB`;
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('ko-KR');
}

function sessionCriteriaLabel(session: ChatSessionListItem): string {
  if(session.claimCriteria.length === 0) {
    return '연결된 약관 없음';
  }
  return session.claimCriteria.map((claimCriteria) => claimCriteria.fileName).join(', ');
}

export default function MyPage() {
  const navigate = useNavigate();
  const { user, loading: authLoading, logout } = useAuth();

  // 목록 데이터
  const [criteria, setCriteria] = useState<ClaimCriteriaListItem[]>([]);
  const [sessions, setSessions] = useState<ChatSessionListItem[]>([]);
  const [dataLoading, setDataLoading] = useState(true);

  // 삭제 대상 (모달 열림 = 값 존재)
  const [deleteTarget, setDeleteTarget] = useState<ClaimCriteriaListItem | null>(null);
  const [deleting, setDeleting] = useState(false);

  // 오류 토스트
  const [toast, setToast] = useState<string | null>(null);

  // 인증 게이트: 로딩이 끝났는데 로그인 사용자가 아니면 /login 으로
  useEffect(() => {
    if(!authLoading && user === null) {
      navigate('/login');
    }
  }, [authLoading, user, navigate]);

  // 로그인 확인 후 두 목록을 병렬 로드
  useEffect(() => {
    if(authLoading || user === null) {
      return;
    }
    let cancelled = false;
    Promise.all([getMyClaimCriteria(), getMyChatSessions()])
      .then(([criteriaList, sessionList]) => {
        if(cancelled) {
          return;
        }
        setCriteria(criteriaList);
        setSessions(sessionList);
      })
      .catch(() => {
        if(!cancelled) {
          setToast('목록을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.');
        }
      })
      .finally(() => {
        if(!cancelled) {
          setDataLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [authLoading, user]);

  async function handleLogout() {
    await logout();
    navigate('/');
  }

  // 모달에서 '삭제' 확정 시 실행
  async function confirmDelete() {
    if(deleteTarget === null) {
      return;
    }
    setDeleting(true);
    try {
      await deleteClaimCriteria(deleteTarget.id);
      // 로컬 목록에서 제거
      setCriteria((prev) => prev.filter((item) => item.id !== deleteTarget.id));
      setDeleteTarget(null);
    } catch {
      setToast('약관 삭제에 실패했습니다. 잠시 후 다시 시도해주세요.');
    } finally {
      setDeleting(false);
    }
  }

  // 인증 확인 전 또는 비로그인(리다이렉트 직전)에는 아무것도 그리지 않음
  if(authLoading || user === null) {
    return (
      <>
        <Header />
        <div className={styles.loading}>불러오는 중...</div>
      </>
    );
  }

  return (
    <>
      <Header />
      <main className={styles.main}>
        {/* 사용자 정보 */}
        <section className={styles.userSection}>
          <div>
            <div className={styles.greeting}>안녕하세요, {user.email}님</div>
            <div className={styles.joinedAt}>가입일 {formatDate(user.createdAt)}</div>
          </div>
          <button className={styles.logout} onClick={handleLogout}>로그아웃</button>
        </section>

        {/* 내 약관 목록 */}
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>내 약관 목록</h2>
          {dataLoading
            ? <div className={styles.empty}>불러오는 중...</div>
            : criteria.length === 0
              ? <div className={styles.empty}>아직 약관이 없습니다.</div>
              : (
                  <div className={styles.list}>
                    {criteria.map((item) => (
                      <div key={item.id} className={styles.criteriaItem}>
                        <div className={styles.criteriaInfo}>
                          <div className={styles.fileName}>{item.fileName}</div>
                          <div className={styles.meta}>
                            {formatFileSize(item.fileSize)} · {formatDate(item.createdAt)}
                          </div>
                        </div>
                        <button
                          className={styles.deleteButton}
                          onClick={() => setDeleteTarget(item)}
                        >
                          삭제
                        </button>
                      </div>
                    ))}
                  </div>
                )}
        </section>

        {/* 최근 상담 내역 */}
        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>최근 상담 내역</h2>
          {dataLoading
            ? <div className={styles.empty}>불러오는 중...</div>
            : sessions.length === 0
              ? <div className={styles.empty}>아직 상담 내역이 없습니다.</div>
              : (
                  <div className={styles.list}>
                    {sessions.map((session) => (
                      <button
                        key={session.id}
                        className={styles.sessionItem}
                        onClick={() => navigate(`/chat/${session.id}`)}
                      >
                        <div className={styles.sessionTop}>
                          <span className={styles.sessionCriteria}>{sessionCriteriaLabel(session)}</span>
                          <span className={styles.sessionDate}>{formatDate(session.createdAt)}</span>
                        </div>
                        {session.lastMessage !== null && (
                          <div className={styles.sessionLast}>{session.lastMessage}</div>
                        )}
                      </button>
                    ))}
                  </div>
                )}
        </section>
      </main>

      {/* 삭제 확인 모달 */}
      <ConfirmModal
        open={deleteTarget !== null}
        title="약관 삭제"
        message={deleteTarget === null
          ? ''
          : `'${deleteTarget.fileName}'을(를) 삭제하시겠습니까? 되돌릴 수 없습니다.`}
        confirmLabel={deleting ? '삭제 중...' : '삭제'}
        onConfirm={confirmDelete}
        onCancel={() => {
          if(!deleting) {
            setDeleteTarget(null);
          }
        }}
      />

      <Toast message={toast} onClose={() => setToast(null)} />
    </>
  );
}