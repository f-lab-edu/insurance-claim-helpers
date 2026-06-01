CREATE TABLE chat_session_claim_criteria (
    chat_session_id   BIGINT NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    claim_criteria_id BIGINT NOT NULL REFERENCES claim_criteria(id) ON DELETE CASCADE,

    PRIMARY KEY (chat_session_id, claim_criteria_id)
);