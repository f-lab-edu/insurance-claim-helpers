import { apiDelete, apiGet } from './client';
import type { ClaimCriteriaStatus } from './chat';

export interface ClaimCriteriaListItem {
  id: number;
  fileName: string;
  fileSize: number;
  status: ClaimCriteriaStatus;
  createdAt: string;
}

export interface ChatSessionClaimCriteria {
  id: number;
  fileName: string;
}

export interface ChatSessionListItem {
  id: number;
  claimCriteria: ChatSessionClaimCriteria[];
  lastMessage: string | null;
  createdAt: string;
}

export async function getMyClaimCriteria(): Promise<ClaimCriteriaListItem[]> {
  const data = await apiGet<ClaimCriteriaListItem[]>('/api/claim-criteria');
  return data ?? [];
}

// 로그인 사용자의 채팅 세션 목록.
export async function getMyChatSessions(): Promise<ChatSessionListItem[]> {
  const data = await apiGet<ChatSessionListItem[]>('/api/chat/sessions');
  return data ?? [];
}

// claim_criteria 삭제 (메타데이터 + vector_store 청크)
export async function deleteClaimCriteria(id: number): Promise<void> {
  return apiDelete(`/api/claim-criteria/${id}`);
}