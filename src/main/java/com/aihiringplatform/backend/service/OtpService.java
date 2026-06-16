package com.aihiringplatform.backend.service;

import com.aihiringplatform.backend.entity.OtpToken;
import com.aihiringplatform.backend.entity.OtpType;
import com.aihiringplatform.backend.repository.OtpRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import org.springframework.transaction.annotation.Transactional;

@Service
public class OtpService {

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;

    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Transactional
    public void generateAndSendOtp(String email, OtpType type) {
        // Enforce 60s cooldown and invalidate existing token
        Optional<OtpToken> existingTokenOpt = otpRepository.findTopByEmailAndTypeOrderByExpiryTimeDesc(email, type);
        if (existingTokenOpt.isPresent() && !existingTokenOpt.get().isVerified() && existingTokenOpt.get().getExpiryTime().isAfter(LocalDateTime.now())) {
            OtpToken old = existingTokenOpt.get();
            if (old.getCreatedAt().plusSeconds(60).isAfter(LocalDateTime.now())) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Please wait 60 seconds before requesting a new OTP.");
            }
            old.setExpiryTime(LocalDateTime.now().minusMinutes(1)); // expire it immediately
            otpRepository.save(old);
        }

        // Generate new 6-digit OTP
        String otpCode = generateSecureOtp();

        // Save hashed OTP
        OtpToken token = new OtpToken();
        token.setEmail(email);
        token.setOtpCode(passwordEncoder.encode(otpCode));
        token.setExpiryTime(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        token.setType(type);
        otpRepository.save(token);

        // Send email ONLY after transaction successfully commits
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    if (type == OtpType.REGISTRATION) {
                        emailService.sendRegistrationOtp(email, otpCode);
                    } else if (type == OtpType.FORGOT_PASSWORD) {
                        emailService.sendForgotPasswordOtp(email, otpCode);
                    }
                }
            });
        } else {
            if (type == OtpType.REGISTRATION) {
                emailService.sendRegistrationOtp(email, otpCode);
            } else if (type == OtpType.FORGOT_PASSWORD) {
                emailService.sendForgotPasswordOtp(email, otpCode);
            }
        }
        
        logger.info("OTP generated for email: {}, type: {}", email, type);
    }

    public boolean verifyOtp(String email, String otpCode, OtpType type) {
        OtpToken token = otpRepository.findTopByEmailAndTypeOrderByExpiryTimeDesc(email, type)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verification code."));

        if (token.isVerified()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP has already been used.");
        }

        if (token.getExpiryTime().isBefore(LocalDateTime.now())) {
            logger.warn("OTP verification failed: expired for email: {}, type: {}", email, type);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification code expired.");
        }

        if (token.getAttempts() >= MAX_ATTEMPTS) {
            logger.warn("OTP verification failed: max attempts reached for email: {}, type: {}", email, type);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Too many failed attempts. Try again later.");
        }

        if (!passwordEncoder.matches(otpCode, token.getOtpCode())) {
            token.setAttempts(token.getAttempts() + 1);
            otpRepository.save(token);
            logger.warn("OTP verification failed: invalid code for email: {}, type: {}", email, type);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid verification code.");
        }

        token.setVerified(true);
        otpRepository.save(token);
        logger.info("OTP successfully verified for email: {}, type: {}", email, type);
        return true;
    }

    public boolean consumeVerifiedOtp(String email, OtpType type) {
        OtpToken token = otpRepository.findTopByEmailAndTypeOrderByExpiryTimeDesc(email, type)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired OTP session."));

        if (!token.isVerified()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP has not been verified yet.");
        }

        if (token.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP session expired. Please start over.");
        }

        // Consume it by deleting it
        otpRepository.delete(token);
        logger.info("Consumed verified OTP for email: {}, type: {}", email, type);
        return true;
    }

    private String generateSecureOtp() {
        SecureRandom random = new SecureRandom();
        int num = random.nextInt(1000000);
        return String.format("%06d", num);
    }
}
