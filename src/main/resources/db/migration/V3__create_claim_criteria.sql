CREATE TABLE claim_criteria (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       REFERENCES users(id) ON DELETE CASCADE,
    session_key VARCHAR(255),
    status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now(),

    -- 로그인(user_id)과 비로그인(session_key) 중 반드시 하나만 존재
    CONSTRAINT chk_claim_criteria_owner
        CHECK (
            (user_id IS NOT NULL AND session_key IS NULL) OR
            (user_id IS NULL AND session_key IS NOT NULL)
        )
);

CREATE INDEX idx_claim_criteria_user_id     ON claim_criteria(user_id);
CREATE INDEX idx_claim_criteria_session_key ON claim_criteria(session_key);