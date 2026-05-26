CREATE TABLE user_oauth_accounts (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider   VARCHAR(50)  NOT NULL,    -- 'google', 'kakao' 등 OAuth 프로바이더 식별자
    oauth_key  VARCHAR(255) NOT NULL,    -- 프로바이더가 발급한 subject ID (예: google_sub)
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at TIMESTAMP    NOT NULL DEFAULT now(),

    -- 동일 프로바이더 + 키 조합은 중복 불가
    CONSTRAINT uq_user_oauth_provider_key UNIQUE (provider, oauth_key)
);

CREATE INDEX idx_user_oauth_accounts_user_id ON user_oauth_accounts(user_id);