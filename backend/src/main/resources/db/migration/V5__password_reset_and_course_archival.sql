-- Flyway Migration V5: Password Reset Tokens, Course Archival & Discussion Enhancements
-- Adds:
-- 1. password_reset_tokens table for secure forgot/reset password workflows
-- 2. archived status column on courses
-- 3. answer endorsement and timestamp tracking on forum answers

-- 1. Password Reset Tokens
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_pwd_reset_tokens_user ON password_reset_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_pwd_reset_tokens_lookup ON password_reset_tokens(token_hash, used, expires_at);

-- 2. Course Archival
ALTER TABLE courses 
    ADD COLUMN IF NOT EXISTS archived BOOLEAN NOT NULL DEFAULT FALSE;

-- 3. Forum Answer Endorsement & Auditing
ALTER TABLE answers 
    ADD COLUMN IF NOT EXISTS is_endorsed BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS endorsed_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
