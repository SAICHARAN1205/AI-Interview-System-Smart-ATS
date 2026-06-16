-- Add rate limiting to users table
ALTER TABLE users ADD COLUMN failed_login_attempts INT DEFAULT 0;
ALTER TABLE users ADD COLUMN account_locked_until TIMESTAMP;

-- Create security logs table
CREATE TABLE security_logs (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255),
    action VARCHAR(255) NOT NULL,
    ip_address VARCHAR(255),
    user_agent VARCHAR(512),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    details TEXT
);

-- Create active sessions table
CREATE TABLE active_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    refresh_token VARCHAR(512) NOT NULL UNIQUE,
    device_info VARCHAR(255),
    ip_address VARCHAR(255),
    last_activity TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_active_session_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_security_logs_email ON security_logs(email);
CREATE INDEX idx_active_sessions_user_id ON active_sessions(user_id);
