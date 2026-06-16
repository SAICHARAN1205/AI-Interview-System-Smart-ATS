package com.aihiringplatform.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SecurityLoggerService {

    private static final Logger logger = LoggerFactory.getLogger("SECURITY_AUDIT");

    @Autowired
    private ActivityLogService activityLogService;

    public void logFailedLogin(String email, String ipAddress, String reason) {
        logger.warn("SECURITY EVENT: Failed Login | Email: {} | IP: {} | Reason: {}", email, ipAddress, reason);
        activityLogService.logSecurityEvent(email, "FAILED_LOGIN", "Failed login attempt. IP: " + ipAddress + " | Reason: " + reason, null);
    }

    public void logAdminAccessViolation(String email, String resource, String ipAddress) {
        logger.warn("SECURITY EVENT: Admin Access Violation | User: {} | Resource: {} | IP: {}", email, resource, ipAddress);
        activityLogService.logSecurityEvent(email, "ADMIN_ACCESS_VIOLATION", "Unauthorized admin access attempt. Resource: " + resource + " | IP: " + ipAddress, null);
    }

    public void logOtpAbuse(String email, String ipAddress) {
        logger.warn("SECURITY EVENT: OTP Abuse Detected | Email: {} | IP: {}", email, ipAddress);
        activityLogService.logSecurityEvent(email, "OTP_ABUSE", "OTP abuse detected. IP: " + ipAddress, null);
    }

    public void logSuspiciousActivity(String email, String activity, String ipAddress) {
        logger.warn("SECURITY EVENT: Suspicious Activity | Email: {} | Activity: {} | IP: {}", email, activity, ipAddress);
        activityLogService.logSecurityEvent(email, "SUSPICIOUS_ACTIVITY", activity + " | IP: " + ipAddress, null);
    }

    public void logJwtFailure(String ipAddress, String reason) {
        logger.warn("SECURITY EVENT: JWT Validation Failure | IP: {} | Reason: {}", ipAddress, reason);
        activityLogService.logSecurityEvent("Anonymous", "JWT_FAILURE", "JWT validation failed. IP: " + ipAddress + " | Reason: " + reason, null);
    }
}
