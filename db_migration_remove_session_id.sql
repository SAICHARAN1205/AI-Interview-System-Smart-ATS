-- =====================================================================
-- DATABASE MIGRATION SCRIPT
-- Objective: Safely remove legacy session-based OTP dependencies
-- =====================================================================

-- 1. Remove session_id from otp_tokens (used in forgot password / generic OTPs)
ALTER TABLE otp_tokens DROP COLUMN IF EXISTS session_id;

-- 2. Remove session_id from pending_registrations (used in registration OTP flow)
ALTER TABLE pending_registrations DROP COLUMN IF EXISTS session_id;

-- Note: The `session_id` column is now fully deprecated as the system
-- has successfully migrated to stateless, database-backed OTP verification
-- using `email`, `otpCode`, and `otpType`.
