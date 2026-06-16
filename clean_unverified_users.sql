-- Delete unverified users who might have been created due to a failed authentication flow
DELETE FROM otp_tokens WHERE email IN (SELECT email FROM users WHERE is_email_verified = false);
DELETE FROM users WHERE is_email_verified = false;
