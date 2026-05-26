CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS vector_store (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content   TEXT,
    metadata  JSONB,
    embedding VECTOR(1536)
);

-- HNSW 인덱스: 코사인 유사도 기반 근사 최근접 이웃 검색
CREATE INDEX ON vector_store USING HNSW (embedding vector_cosine_ops);