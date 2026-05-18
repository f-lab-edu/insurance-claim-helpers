# Insurance Claim Helpers

보험 약관 PDF를 분석해 사전에 청구 가능 여부를 판단하고 가입자 편에서 약관을 해석해주는 서비스

---

## 주요 기능

- 보험 약관 PDF 업로드 및 분석
- 치료 전/후 보험 청구 가능 여부 사전 확인
- 약관 원문 근거 조항 제시
- 대화형 챗봇으로 자유로운 질문
- 비로그인으로도 사용 가능 / 로그인 시 분석 이력 저장

## 기술 스택

| 구분 | 기술 |
|------|------|
| Backend | Java 21, Spring Boot |
| Frontend | React |
| AI (생성) | Claude API (Anthropic) |
| AI (검색) | OpenAI Embedding |
| Database | PostgreSQL 16, pgVector |

## 아키텍처

[아키텍처 문서 보기](docs/specs/2026-04-20-architecture.md)

## 로컬 개발 환경 구성

**사전 요구사항:** JDK 21, Docker Desktop, OpenAI/Anthropic API Key

### 1. 인프라 실행

```bash
docker compose up -d
```

### 2. API 키 설정

**IntelliJ:** Run Configuration → Environment Variables

| 변수명 | 값 |
|--------|-----|
| `SPRING_AI_OPENAI_API_KEY` | OpenAI API Key |
| `SPRING_AI_ANTHROPIC_API_KEY` | Anthropic API Key |

**CLI:**
```bash
export SPRING_AI_OPENAI_API_KEY=sk-...
export SPRING_AI_ANTHROPIC_API_KEY=sk-ant-...
```

### 3. 앱 실행

DB 접속 정보는 `application.yml` 기본값이 docker-compose 설정과 일치하므로 별도 설정 불필요.

```bash
./gradlew bootRun
```