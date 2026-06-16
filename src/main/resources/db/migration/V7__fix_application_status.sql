-- Fix invalid application statuses introduced by enum rename
UPDATE applications SET status = 'INTERVIEW_SCHEDULED' WHERE status = 'INTERVIEW';
