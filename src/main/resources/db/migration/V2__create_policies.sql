CREATE TABLE policies (
    id          UUID         PRIMARY KEY,
    user_id     UUID         REFERENCES users(id) ON DELETE CASCADE,
    session_key VARCHAR(255),
    file_name   VARCHAR(255) NOT NULL,
    file_size   BIGINT       NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now(),

    -- 로그인(user_id)과 비로그인(session_key) 중 반드시 하나만 존재
    CONSTRAINT chk_policies_owner
        CHECK (
            (user_id IS NOT NULL AND session_key IS NULL) OR
            (user_id IS NULL AND session_key IS NOT NULL)
        )
);

CREATE INDEX idx_policies_user_id     ON policies(user_id);
CREATE INDEX idx_policies_session_key ON policies(session_key);