package com.aihiringplatform.backend.service;

import com.aihiringplatform.backend.repository.OtpRepository;
import com.aihiringplatform.backend.repository.PendingRegistrationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class CleanupService {

    private static final Logger logger = LoggerFactory.getLogger(CleanupService.class);

    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    // Run every 1 minute
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cleanupExpiredTokensAndRegistrations() {
        LocalDateTime now = LocalDateTime.now();
        
        try {
            otpRepository.deleteByExpiryTimeBefore(now);
            pendingRegistrationRepository.deleteByExpiryTimeBefore(now);
            logger.debug("Successfully cleaned up expired OTPs and pending registrations.");
        } catch (Exception e) {
            logger.error("Error occurred during cleanup of expired records.", e);
        }
    }
}
