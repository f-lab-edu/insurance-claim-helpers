import { apiDelete, apiGet, apiPost, apiUpload, ApiError, toApiError } from './client';

export type ClaimCriteriaStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

export type MessageRole = 'USER' | 'ASSISTANT';

// 세션에 연결된 약관 칩 데이터 (ChatSessionDetailResponse.claimCriteria 요소).
export interface ClaimCriteriaChipData {
  id: number;
  fileName: string;
  status: ClaimCriteriaStatus;
}

// 메시지 이력 한 건 (ChatMessageResponse).
export interface ChatMessage {
  id: number;
  role: MessageRole;
  content: string;
  createdAt: string;
}

// GET /api/chat/sessions/{id} 응답 (ChatSessionDetailResponse).
export interface ChatSessionDetail {
  id: number;
  claimCriteria: ClaimCriteriaChipData[];
  messages: ChatMessage[];
}

// POST /api/claim-criteria 업로드 응답 (ClaimCriteriaUploadResponse).
export interface ClaimCriteriaUploadResponse {
  id: number;
  fileName: string;
  fileSize: number;
  status: ClaimCriteriaStatus;
  createdAt: string;
}

// GET /api/claim-criteria/{id}/status 응답 (ClaimCriteriaStatusResponse).
export interface ClaimCriteriaStatusResponse {
  id: number;
  status: ClaimCriteriaStatus;
}

// POST /api/chat/sessions/{id}/claim-criteria 응답 (ClaimCriteriaAttachResponse).
export interface ClaimCriteriaAttachResponse {
  chatSessionId: number;
  claimCriteriaId: number;
  fileName: string;
}

// --- 파일 사전 검증 ---

const PDF_CONTENT_TYPE = 'application/pdf';

export const MAX_FILE_BYTES = 30 * 1024 * 1024 * 0.8;

export function validatePdfFile(file: File): string | null {
  if(file.type !== PDF_CONTENT_TYPE) {
    return 'INVALID_FILE_TYPE';
  }
  if(file.size > MAX_FILE_BYTES) {
    return 'FILE_SIZE_EXCEEDED';
  }
  return null;
}

// --- API 함수 ---

export async function getSession(sessionId: string): Promise<ChatSessionDetail> {
  const data = await apiGet<ChatSessionDetail>(`/api/chat/sessions/${sessionId}`);
  if(data === null) {
    throw new ApiError('SESSION_NOT_FOUND', 404, '세션을 불러오지 못했습니다.');
  }
  return data;
}

export async function uploadClaimCriteria(file: File): Promise<ClaimCriteriaUploadResponse> {
  const formData = new FormData();
  formData.append('file', file);
  return apiUpload<ClaimCriteriaUploadResponse>('/api/claim-criteria', formData);
}

// 처리 상태 1회 조회.
export async function getClaimCriteriaStatus(id: number): Promise<ClaimCriteriaStatus> {
  const data = await apiGet<ClaimCriteriaStatusResponse>(`/api/claim-criteria/${id}/status`);
  if(data === null) {
    throw new ApiError('CLAIM_CRITERIA_NOT_FOUND', 404, '약관을 찾을 수 없습니다.');
  }
  return data.status;
}

// 업로드 후 임베딩이 끝날 때까지(COMPLETED) 상태를 폴링.
//   - COMPLETED : 정상 반환
//   - FAILED    : ApiError('CLAIM_CRITERIA_FAILED') 로 중단
//   - 타임아웃   : ApiError('POLL_TIMEOUT') 로 중단
// intervalMs 간격으로 timeoutMs 까지 재시도.
export async function pollUntilCompleted(
  id: number,
  intervalMs = 1500,
  timeoutMs = 300000,
): Promise<void> {
  const deadline = Date.now() + timeoutMs;

  while(Date.now() < deadline) {
    const status = await getClaimCriteriaStatus(id);

    if(status === 'COMPLETED') {
      return;
    }
    if(status === 'FAILED') {
      throw new ApiError('CLAIM_CRITERIA_FAILED', 0, '약관 처리에 실패했습니다.');
    }
    await delay(intervalMs);
  }

  throw new ApiError('POLL_TIMEOUT', 0, '약관 처리 시간이 초과되었습니다.');
}

// 세션에 약관 연결.
export async function attachClaimCriteria(
  sessionId: string,
  claimCriteriaId: number,
): Promise<ClaimCriteriaAttachResponse> {
  return apiPost<ClaimCriteriaAttachResponse>(
    `/api/chat/sessions/${sessionId}/claim-criteria`,
    { claimCriteriaId },
  );
}

// 세션에서 약관 연결 해제.
export async function detachClaimCriteria(sessionId: string, claimCriteriaId: number): Promise<void> {
  return apiDelete(`/api/chat/sessions/${sessionId}/claim-criteria/${claimCriteriaId}`);
}

// SSE 스트리밍 수신 콜백.
export interface StreamHandlers {
  onDelta: (delta: string) => void;
  onDone: (messageId: number) => void;
  onError: (error: unknown) => void;
}

// 메시지 전송 + AI 답변 SSE 스트리밍.
export async function streamMessage(
  sessionId: string,
  content: string,
  handlers: StreamHandlers,
  signal?: AbortSignal,
): Promise<void> {
  try {
    const response = await fetch(`/api/chat/sessions/${sessionId}/messages`, {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
      },
      body: JSON.stringify({ content }),
      signal,
    });

    if(!response.ok || response.body === null) {
      throw await toApiError(response);
    }

    await readSseStream(response.body, handlers);
  } catch(error) {
      
    if(signal?.aborted) {
      return;
    }
    handlers.onError(error);
  }
}

async function readSseStream(body: ReadableStream<Uint8Array>, handlers: StreamHandlers): Promise<void> {
  const reader = body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  let completed = false;
  
  for(;;) {
    const { value, done } = await reader.read();
    if(done) {
      break;
    }

    buffer += decoder.decode(value, { stream: true });

    let boundary = buffer.indexOf('\n\n');
    while(boundary !== -1) {
      const rawEvent = buffer.slice(0, boundary);
      buffer = buffer.slice(boundary + 2);

      const payload = parseSseData(rawEvent);
      if(payload !== null) {
        if(isDone(payload)) {
          completed = true;
          handlers.onDone(payload.messageId);
        } else if(isDelta(payload)) {
          handlers.onDelta(payload.delta);
        }
      }

      boundary = buffer.indexOf('\n\n');
    }
  }

  if(!completed) {
    throw new ApiError('STREAM_INTERRUPTED', 0, '답변 도중 연결이 끊겼습니다.');
  }
}

function parseSseData(rawEvent: string): { delta: string } | { done: boolean; messageId: number } | null {
  const dataLines = rawEvent
    .split('\n')
    .filter((line) => line.startsWith('data:'))
    .map((line) => line.replace(/^data:\s?/, ''));

  if(dataLines.length === 0) {
    return null;
  }

  try {
    return JSON.parse(dataLines.join('\n'));
  } catch {
    return null;
  }
}

function isDone(payload: object): payload is { done: boolean; messageId: number } {
  return 'done' in payload;
}

function isDelta(payload: object): payload is { delta: string } {
  return 'delta' in payload;
}

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}