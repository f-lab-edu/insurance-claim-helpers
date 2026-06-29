-- user_oauth_accounts.user_id → users
ALTER TABLE user_oauth_accounts DROP CONSTRAINT user_oauth_accounts_user_id_fkey;
ALTER TABLE user_oauth_accounts
    ADD CONSTRAINT user_oauth_accounts_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES users(id);

-- claim_criteria.user_id → users
ALTER TABLE claim_criteria DROP CONSTRAINT claim_criteria_user_id_fkey;
ALTER TABLE claim_criteria
    ADD CONSTRAINT claim_criteria_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES users(id);

-- documents.claim_criteria_id → claim_criteria
ALTER TABLE documents DROP CONSTRAINT documents_claim_criteria_id_fkey;
ALTER TABLE documents
    ADD CONSTRAINT documents_claim_criteria_id_fkey
        FOREIGN KEY (claim_criteria_id) REFERENCES claim_criteria(id);

-- chat_sessions.user_id → users
ALTER TABLE chat_sessions DROP CONSTRAINT chat_sessions_user_id_fkey;
ALTER TABLE chat_sessions
    ADD CONSTRAINT chat_sessions_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES users(id);

-- chat_session_claim_criteria.chat_session_id → chat_sessions
ALTER TABLE chat_session_claim_criteria DROP CONSTRAINT chat_session_claim_criteria_chat_session_id_fkey;
ALTER TABLE chat_session_claim_criteria
    ADD CONSTRAINT chat_session_claim_criteria_chat_session_id_fkey
        FOREIGN KEY (chat_session_id) REFERENCES chat_sessions(id);

-- chat_session_claim_criteria.claim_criteria_id → claim_criteria
ALTER TABLE chat_session_claim_criteria DROP CONSTRAINT chat_session_claim_criteria_claim_criteria_id_fkey;
ALTER TABLE chat_session_claim_criteria
    ADD CONSTRAINT chat_session_claim_criteria_claim_criteria_id_fkey
        FOREIGN KEY (claim_criteria_id) REFERENCES claim_criteria(id);

-- chat_messages.chat_session_id → chat_sessions
ALTER TABLE chat_messages DROP CONSTRAINT chat_messages_chat_session_id_fkey;
ALTER TABLE chat_messages
    ADD CONSTRAINT chat_messages_chat_session_id_fkey
        FOREIGN KEY (chat_session_id) REFERENCES chat_sessions(id);