interface GetOptions {
  //   true  → 401이면 throw 대신 null 반환
  //   false → 401도 일반 오류로 취급해 throw
  allow401?: boolean;
}

// GET 요청을 보내고 JSON 본문을 T 타입으로 반환
export async function apiGet<T>(path: string, options: GetOptions = {}): Promise<T | null> {
  const response = await fetch(path, {
    method: 'GET',
    // 다른 출처(dev 프록시)를 거쳐도 세션 쿠키가 전송되도록 보장
    credentials: 'include',
  });

  if(response.status === 401 && options.allow401) {
    return null;
  }

  if(!response.ok) {
    throw new Error(`GET ${path} 실패: ${response.status}`);
  }

  return (await response.json()) as T;
}

//   - /api/chat/sessions : 본문 없이 호출 → { id } 응답 파싱
//   - /auth/logout       : 204 No Content → 파싱하지 않고 undefined 반환
export async function apiPost<T>(path: string, body?: unknown): Promise<T> {
  const response = await fetch(path, {
    method: 'POST',
    credentials: 'include',
    headers: body !== undefined ? { 'Content-Type': 'application/json' } : undefined,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if(!response.ok) {
    throw new Error(`POST ${path} 실패: ${response.status}`);
  }

  if(response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}