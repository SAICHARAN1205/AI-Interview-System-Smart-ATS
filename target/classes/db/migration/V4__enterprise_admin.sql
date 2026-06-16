-- Phase 1: Enterprise Admin Infrastructure

-- Update users table with account_status
ALTER TABLE users ADD COLUMN IF NOT EXISTS account_status VARCHAR(255) DEFAULT 'PENDING_VERIFICATION' NOT NULL;
UPDATE users SET account_status = 'ACTIVE' WHERE account_status = 'PENDING_VERIFICATION';

-- Create recruiter_profiles table if not exists
CREATE TABLE IF NOT EXISTS recruiter_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    company_name VARCHAR(255),
    designation VARCHAR(255),
    company_website VARCHAR(255),
    linked_in_profile VARCHAR(255),
    hiring_department VARCHAR(255),
    company_description TEXT,
    company_location VARCHAR(255),
    company_logo_path VARCHAR(255),
    contact_number VARCHAR(255),
    verification_status VARCHAR(255) DEFAULT 'PENDING' NOT NULL,
    CONSTRAINT fk_recruiter_profile_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Ensure columns exist in case table was created earlier without them
ALTER TABLE recruiter_profiles ADD COLUMN IF NOT EXISTS verification_status VARCHAR(255) DEFAULT 'PENDING' NOT NULL;

-- Create ai_provider_configs table
CREATE TABLE IF NOT EXISTS ai_provider_configs (
    id BIGSERIAL PRIMARY KEY,
    provider_name VARCHAR(255) NOT NULL UNIQUE,
    is_enabled BOOLEAN DEFAULT TRUE NOT NULL,
    priority_order INT DEFAULT 0 NOT NULL,
    total_tokens_used BIGINT DEFAULT 0 NOT NULL,
    failure_count INT DEFAULT 0 NOT NULL
);

-- Insert default AI providers safely
INSERT INTO ai_provider_configs (provider_name, is_enabled, priority_order) 
SELECT 'Gemini', TRUE, 1 WHERE NOT EXISTS (SELECT 1 FROM ai_provider_configs WHERE provider_name = 'Gemini');
INSERT INTO ai_provider_configs (provider_name, is_enabled, priority_order) 
SELECT 'OpenAI', TRUE, 2 WHERE NOT EXISTS (SELECT 1 FROM ai_provider_configs WHERE provider_name = 'OpenAI');
INSERT INTO ai_provider_configs (provider_name, is_enabled, priority_order) 
SELECT 'DeepSeek', TRUE, 3 WHERE NOT EXISTS (SELECT 1 FROM ai_provider_configs WHERE provider_name = 'DeepSeek');
INSERT INTO ai_provider_configs (provider_name, is_enabled, priority_order) 
SELECT 'Groq', TRUE, 4 WHERE NOT EXISTS (SELECT 1 FROM ai_provider_configs WHERE provider_name = 'Groq');
INSERT INTO ai_provider_configs (provider_name, is_enabled, priority_order) 
SELECT 'Together AI', TRUE, 5 WHERE NOT EXISTS (SELECT 1 FROM ai_provider_configs WHERE provider_name = 'Together AI');
INSERT INTO ai_provider_configs (provider_name, is_enabled, priority_order) 
SELECT 'OpenRouter', TRUE, 6 WHERE NOT EXISTS (SELECT 1 FROM ai_provider_configs WHERE provider_name = 'OpenRouter');
INSERT INTO ai_provider_configs (provider_name, is_enabled, priority_order) 
SELECT 'SmartATS local fallback', TRUE, 7 WHERE NOT EXISTS (SELECT 1 FROM ai_provider_configs WHERE provider_name = 'SmartATS local fallback');

-- Security logs table
CREATE TABLE IF NOT EXISTS security_logs (
    id BIGSERIAL PRIMARY KEY,
    action VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    ip_address VARCHAR(255) NOT NULL,
    user_agent VARCHAR(512),
    details TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_security_logs_email ON security_logs(email);
