CREATE TABLE chat_sessions (
    id          UUID         PRIMARY KEY,
    user_id     UUID         REFERENCES users(id) ON DELETE CASCADE,
    session_key VARCHAR(255),
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now(),

    -- 로그인(user_id)과 비로그인(session_key) 중 반드시 하나만 존재
    CONSTRAINT chk_chat_sessions_owner
        CHECK (
            (user_id IS NOT NULL AND session_key IS NULL) OR
            (user_id IS NULL AND session_key IS NOT NULL)
        )
);

CREATE INDEX idx_chat_sessions_user_id     ON chat_sessions(user_id);
CREATE INDEX idx_chat_sessions_session_key ON chat_sessions(session_key);