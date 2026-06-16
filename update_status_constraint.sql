-- Safely update the applications_status_check constraint to include new statuses
-- (INTERVIEW, HIRED) without dropping application data.

ALTER TABLE applications DROP CONSTRAINT IF EXISTS applications_status_check;

ALTER TABLE applications ADD CONSTRAINT applications_status_check 
CHECK (status IN ('APPLIED', 'UNDER_REVIEW', 'SHORTLISTED', 'INTERVIEW', 'REJECTED', 'HIRED'));
