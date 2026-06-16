package com.aihiringplatform.backend.service;

import com.aihiringplatform.backend.entity.ActivityLog;
import com.aihiringplatform.backend.repository.ActivityLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class ActivityLogService {

    private static final Logger logger = LoggerFactory.getLogger(ActivityLogService.class);

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Async
    public void logActivity(String email, String role, String actionType, String description, String status, Map<String, Object> metadata, HttpServletRequest request) {
        try {
            ActivityLog log = new ActivityLog();
            log.setUserEmail(email);
            log.setUserRole(role);
            log.setActionType(actionType);
            log.setActionDescription(description);
            log.setStatus(status);

            if (request != null) {
                log.setIpAddress(extractIpAddress(request));
                log.setUserAgent(request.getHeader("User-Agent"));
            }

            if (metadata != null && !metadata.isEmpty()) {
                try {
                    log.setMetadata(objectMapper.writeValueAsString(metadata));
                } catch (JsonProcessingException e) {
                    logger.warn("Failed to serialize metadata for activity log: {}", e.getMessage());
                }
            }

            activityLogRepository.save(log);
        } catch (Exception e) {
            // NEVER block main thread, just log to file
            logger.error("CRITICAL: Failed to save activity log for {}: {}", email, e.getMessage(), e);
        }
    }

    @Async
    public void logSuccess(String email, String role, String actionType, String description, HttpServletRequest request) {
        logActivity(email, role, actionType, description, "SUCCESS", null, resolveRequest(request));
    }

    @Async
    public void logFailure(String email, String role, String actionType, String description, HttpServletRequest request) {
        logActivity(email, role, actionType, description, "FAILURE", null, resolveRequest(request));
    }

    @Async
    public void logSecurityEvent(String email, String actionType, String description, HttpServletRequest request) {
        logActivity(email, "UNKNOWN", actionType, description, "SECURITY_EVENT", null, resolveRequest(request));
    }

    private HttpServletRequest resolveRequest(HttpServletRequest request) {
        if (request != null) {
            return request;
        }
        try {
            org.springframework.web.context.request.ServletRequestAttributes attributes = 
                (org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return attributes.getRequest();
            }
        } catch (Exception e) {
            // Not in web context
        }
        return null;
    }

    private String extractIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @Scheduled(cron = "0 0 0 * * ?") // Runs daily at midnight
    @Transactional
    public void cleanupOldLogs() {
        try {
            LocalDateTime ninetyDaysAgo = LocalDateTime.now().minusDays(90);
            activityLogRepository.deleteByCreatedAtBefore(ninetyDaysAgo);
            logger.info("Successfully cleaned up activity logs older than 90 days.");
        } catch (Exception e) {
            logger.error("Failed to clean up old activity logs: {}", e.getMessage(), e);
        }
    }
}
