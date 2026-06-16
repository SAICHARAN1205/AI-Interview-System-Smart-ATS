-- Ensure columns exist in case table was created earlier without them
ALTER TABLE recruiter_profiles ADD COLUMN IF NOT EXISTS verification_status VARCHAR(255) DEFAULT 'PENDING' NOT NULL;

-- Make sure index exists on security_logs (email)
CREATE INDEX IF NOT EXISTS idx_security_logs_email ON security_logs(email);

-- Drop and recreate the role check constraint to include ADMIN
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN ('CANDIDATE', 'RECRUITER', 'ADMIN'));
