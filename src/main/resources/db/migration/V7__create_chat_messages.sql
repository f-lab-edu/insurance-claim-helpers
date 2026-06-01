CREATE TABLE chat_messages (
    id              BIGSERIAL   PRIMARY KEY,
    chat_session_id BIGINT      NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    role            VARCHAR(20) NOT NULL,
    content         TEXT        NOT NULL,
    created_at      TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX idx_chat_messages_chat_session_id ON chat_messages(chat_session_id);