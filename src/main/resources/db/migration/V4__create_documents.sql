CREATE TABLE documents (
    id                BIGSERIAL     PRIMARY KEY,
    claim_criteria_id BIGINT        NOT NULL UNIQUE REFERENCES claim_criteria(id) ON DELETE CASCADE,
    file_name         VARCHAR(255)  NOT NULL,
    file_size         BIGINT        NOT NULL,
    object_key        VARCHAR(1000),    -- S3 오브젝트 키 (업로드 전에는 NULL)
    created_at        TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP     NOT NULL DEFAULT now()
);