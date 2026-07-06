import { useCallback, useEffect, useRef, useState, type DragEvent } from 'react';
import { useParams } from 'react-router-dom';
import Header from '../components/Header';
import MessageBubble from '../components/MessageBubble';
import ClaimCriteriaChip from '../components/ClaimCriteriaChip';
import MessageComposer from '../components/MessageComposer';
import Toast from '../components/Toast';
import { ApiError } from '../api/client';
import {
  attachClaimCriteria,
  detachClaimCriteria,
  getSession,
  pollUntilCompleted,
  streamMessage,
  uploadClaimCriteria,
  validatePdfFile,
  type ChatMessage,
  type ClaimCriteriaChipData,
} from '../api/chat';
import styles from './ChatPage.module.css';

const ERROR_MESSAGES: Record<string, string> = {
  INVALID_FILE_TYPE: 'PDF 파일만 업로드할 수 있습니다.',
  FILE_SIZE_EXCEEDED: '파일 크기 제한(24MB)을 초과했습니다.',
  CLAIM_CRITERIA_NOT_COMPLETED: '약관 처리가 아직 완료되지 않았습니다.',
  CLAIM_CRITERIA_FAILED: '약관 처리에 실패했습니다. 다른 파일로 다시 시도해주세요.',
  CLAIM_CRITERIA_NOT_FOUND: '약관을 찾을 수 없습니다.',
  POLL_TIMEOUT: '약관 처리 시간이 초과되었습니다. 잠시 후 다시 시도해주세요.',
  SESSION_NOT_FOUND: '채팅 세션을 찾을 수 없습니다.',
  NO_CLAIM_CRITERIA_ATTACHED: '먼저 약관을 첨부해주세요.',
  FORBIDDEN: '접근 권한이 없습니다.',
  STREAM_INTERRUPTED: '답변 도중 연결이 끊겼습니다. 다시 시도해주세요.',
  UNAUTHORIZED: '로그인이 필요합니다.',
};

function errorToMessage(error: unknown): string {
  if(error instanceof ApiError) {
    return ERROR_MESSAGES[error.code] ?? error.message ?? '요청 처리 중 오류가 발생했습니다.';
  }
  return '요청 처리 중 오류가 발생했습니다.';
}

type DisplayMessage = ChatMessage & { streaming?: boolean };

export default function ChatPage() {
  const { sessionId } = useParams<{ sessionId: string }>();

  // 진입 초기화 상태
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(false);

  // 화면 상태
  const [claimCriteria, setClaimCriteria] = useState<ClaimCriteriaChipData[]>([]);
  const [messages, setMessages] = useState<DisplayMessage[]>([]);

  // 진행 중 플래그 — 입력/첨부 버튼 비활성화에 사용
  const [uploading, setUploading] = useState(false);
  const [streaming, setStreaming] = useState(false);
  const [removingId, setRemovingId] = useState<number | null>(null);
  const [dragOver, setDragOver] = useState(false);

  const [toast, setToast] = useState<string | null>(null);

  // 숨겨진 파일 input, 스크롤 최하단 sentinel, 스트림 취소 컨트롤러
  const fileInputRef = useRef<HTMLInputElement>(null);
  const bottomRef = useRef<HTMLDivElement>(null);
  const abortRef = useRef<AbortController | null>(null);

  // State A(약관 미첨부) / State B(첨부됨) 분기
  const isStateA = claimCriteria.length === 0;

  const closeToast = useCallback(() => setToast(null), []);

  // --- 진입 초기화: 세션 상세 로드 ---
  useEffect(() => {
    if(sessionId === undefined) {
      return;
    }
    let cancelled = false;

    getSession(sessionId)
      .then((detail) => {
        if(cancelled) {
          return;
        }
        setClaimCriteria(detail.claimCriteria);
        setMessages(detail.messages);
      })
      .catch(() => {
        if(!cancelled) {
          setLoadError(true);
        }
      })
      .finally(() => {
        if(!cancelled) {
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [sessionId]);

  // --- 메시지 변경 시 자동 스크롤 ---
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // --- 언마운트 시 진행 중 스트림 취소 ---
  useEffect(() => {
    return () => abortRef.current?.abort();
  }, []);

  // --- 약관 첨부: 업로드 → 상태 폴링 → 세션 연결 ---
  async function handleFile(file: File) {
    // 서버 호출 전 1차 검증(타입/크기)
    const invalidCode = validatePdfFile(file);
    if(invalidCode !== null) {
      setToast(ERROR_MESSAGES[invalidCode]);
      return;
    }

    setUploading(true);
    try {
      const uploaded = await uploadClaimCriteria(file);
      await pollUntilCompleted(uploaded.id);
      const attached = await attachClaimCriteria(sessionId!, uploaded.id);
      setClaimCriteria((prev) => [
        ...prev,
        { id: attached.claimCriteriaId, fileName: attached.fileName, status: 'COMPLETED' },
      ]);
    } catch(error) {
      setToast(errorToMessage(error));
    } finally {
      setUploading(false);
    }
  }

  // --- 약관 연결 해제 ---
  async function handleRemove(id: number) {
    setRemovingId(id);
    try {
      await detachClaimCriteria(sessionId!, id);
      setClaimCriteria((prev) => prev.filter((criteria) => criteria.id !== id));
    } catch(error) {
      setToast(errorToMessage(error));
    } finally {
      setRemovingId(null);
    }
  }

  // --- 메시지 전송 + SSE 스트리밍 ---
  function handleSend(content: string) {
    const base = Date.now();
    const userMessage: DisplayMessage = {
      id: -base,
      role: 'USER',
      content,
      createdAt: new Date().toISOString(),
    };
    const assistantTempId = -(base + 1);
    const assistantMessage: DisplayMessage = {
      id: assistantTempId,
      role: 'ASSISTANT',
      content: '',
      createdAt: new Date().toISOString(),
      streaming: true,
    };
    setMessages((prev) => [...prev, userMessage, assistantMessage]);
    setStreaming(true);

    const controller = new AbortController();
    abortRef.current = controller;

    streamMessage(
      sessionId!,
      content,
      {
        onDelta: (delta) => {
          setMessages((prev) =>
            prev.map((message) =>
              message.id === assistantTempId
                ? { ...message, content: message.content + delta }
                : message,
            ),
          );
        },
        onDone: (messageId) => {
          setMessages((prev) =>
            prev.map((message) =>
              message.id === assistantTempId
                ? { ...message, id: messageId, streaming: false }
                : message,
            ),
          );
          setStreaming(false);
        },
        onError: (error) => {
          setMessages((prev) =>
            prev.filter((message) => !(message.id === assistantTempId && message.content === '')),
          );
          setToast(errorToMessage(error));
          setStreaming(false);
        },
      },
      controller.signal,
    );
  }

  // --- 파일 선택/드롭 진입점 ---
  function openFilePicker() {
    fileInputRef.current?.click();
  }

  function handleInputChange(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if(file) {
      void handleFile(file);
    }
    event.target.value = '';
  }

  function handleDrop(event: DragEvent<HTMLDivElement>) {
    event.preventDefault();
    setDragOver(false);
    // 업로드 중이면 무시
    if(uploading) {
      return;
    }
    const file = event.dataTransfer.files?.[0];
    if(file) {
      void handleFile(file);
    }
  }

  function handleDragOver(event: DragEvent<HTMLDivElement>) {
    event.preventDefault();
    setDragOver(true);
  }

  function handleDragLeave() {
    setDragOver(false);
  }

  // 입력창 비활성 조건: State A / 업로드 중 / 스트리밍 중
  const composerDisabled = isStateA || uploading || streaming;
  const composerPlaceholder = isStateA ? '약관을 먼저 첨부하세요' : '메시지를 입력하세요';

  return (
    <div className={styles.page}>
      <Header />

      {loading ? (
        <div className={styles.centered}>불러오는 중...</div>
      ) : loadError ? (
        <div className={styles.centered}>채팅 세션을 불러오지 못했습니다.</div>
      ) : (
        <>
          {/* 첨부 약관 칩 바 (State B 에서만) */}
          {!isStateA && (
            <div className={styles.chipBar}>
              {claimCriteria.map((criteria) => (
                <ClaimCriteriaChip
                  key={criteria.id}
                  fileName={criteria.fileName}
                  onRemove={() => void handleRemove(criteria.id)}
                  removing={removingId === criteria.id}
                />
              ))}
              <button
                type="button"
                className={styles.addButton}
                onClick={openFilePicker}
                disabled={uploading}
              >
                {uploading ? '처리 중...' : '+ PDF 추가'}
              </button>
            </div>
          )}

          <div
            className={`${styles.body} ${dragOver ? styles.dragOver : ''}`}
            onDrop={handleDrop}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
          >
            {isStateA ? (
              // State A: 약관 첨부 안내 카드
              <div className={styles.attachCard}>
                <p className={styles.attachTitle}>보험 약관 PDF를 첨부하면</p>
                <p className={styles.attachTitle}>청구 가능 여부를 확인해드려요</p>
                <button
                  type="button"
                  className={styles.attachButton}
                  onClick={openFilePicker}
                  disabled={uploading}
                >
                  {uploading ? '처리 중...' : '📎 PDF 파일 첨부하기'}
                </button>
              </div>
            ) : (
              // State B: 메시지 이력
              <div className={styles.messages}>
                {messages.map((message) => (
                  <MessageBubble
                    key={message.id}
                    role={message.role}
                    content={message.content}
                    streaming={message.streaming}
                  />
                ))}
                <div ref={bottomRef} />
              </div>
            )}
          </div>

          {/* 하단 입력창 */}
          <MessageComposer
            disabled={composerDisabled}
            placeholder={composerPlaceholder}
            onSend={handleSend}
          />
        </>
      )}

      {/* 숨겨진 파일 input — 버튼/드롭이 공유 */}
      <input
        ref={fileInputRef}
        type="file"
        accept="application/pdf,.pdf"
        hidden
        onChange={handleInputChange}
      />

      <Toast message={toast} onClose={closeToast} />
    </div>
  );
}