CREATE TABLE chat_session_policies (
    chat_session_id UUID NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    policy_id       UUID NOT NULL REFERENCES policies(id) ON DELETE CASCADE,

    PRIMARY KEY (chat_session_id, policy_id)
);