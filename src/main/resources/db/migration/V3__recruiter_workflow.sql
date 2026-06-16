-- Phase 1 & 2: Recruiter Workflow & ATS Hardening

-- Update jobs table with new configuration fields
ALTER TABLE jobs ADD COLUMN ats_strictness_level VARCHAR(255) DEFAULT 'STANDARD' NOT NULL;
ALTER TABLE jobs ADD COLUMN interview_rounds INT DEFAULT 1 NOT NULL;

-- Ensure applications table has the new status values and recruiter notes
ALTER TABLE applications ADD COLUMN recruiter_notes TEXT;

-- Update resumes table with file hashing to detect duplicates
ALTER TABLE resumes ADD COLUMN file_hash VARCHAR(64);

-- Create application_history_logs to track recruiter actions
CREATE TABLE application_history_logs (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL,
    changed_by_user_id BIGINT NOT NULL,
    old_status VARCHAR(255),
    new_status VARCHAR(255) NOT NULL,
    comments TEXT,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_history_application FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE,
    CONSTRAINT fk_history_changed_by FOREIGN KEY (changed_by_user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_application_history_app_id ON application_history_logs(application_id);
CREATE INDEX idx_resumes_file_hash ON resumes(file_hash);
