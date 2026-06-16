package com.aihiringplatform.backend.service;

import com.aihiringplatform.backend.entity.User;
import com.aihiringplatform.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.aihiringplatform.backend.entity.OtpType;
import com.aihiringplatform.backend.entity.UserStatus;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aihiringplatform.backend.repository.PendingRegistrationRepository;
import com.aihiringplatform.backend.repository.OtpRepository;
import com.aihiringplatform.backend.repository.ActiveSessionRepository;
import com.aihiringplatform.backend.entity.PendingRegistration;
import java.time.LocalDateTime;
import java.util.regex.Pattern;
import java.util.List;
import java.util.UUID;
import org.springframework.scheduling.annotation.Scheduled;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private OtpService otpService;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private ActiveSessionRepository activeSessionRepository;

    @Autowired
    private ActivityLogService activityLogService;

    private static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$";
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(PASSWORD_REGEX);

    private void validatePassword(String password) {
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Weak password. Must contain at least 8 characters, one uppercase letter, one lowercase letter, one number, and one special character (e.g. _, @, #, etc).");
        }
    }

    @Transactional
    public java.util.Map<String, Object> registerUser(User user) {
        validatePassword(user.getPassword());

        java.util.Optional<User> existingUserOpt = userRepository.findByEmail(user.getEmail());
        if (existingUserOpt.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account already exists.");
        }

        java.util.Optional<PendingRegistration> existingPendingOpt = pendingRegistrationRepository.findByEmail(user.getEmail());

        String sessionId;
        if (existingPendingOpt.isPresent()) {
            // REUSE existing session ID if there is already a pending registration
            sessionId = existingPendingOpt.get().getSessionId();
            if (sessionId == null || sessionId.isEmpty()) {
                sessionId = UUID.randomUUID().toString();
            }
        } else {
            sessionId = UUID.randomUUID().toString();
        }

        PendingRegistration pendingRegistration = existingPendingOpt.orElse(new PendingRegistration());
        pendingRegistration.setEmail(user.getEmail());
        pendingRegistration.setName(user.getName());
        pendingRegistration.setPassword(passwordEncoder.encode(user.getPassword()));
        pendingRegistration.setRole(user.getRole());
        pendingRegistration.setCreatedAt(LocalDateTime.now());
        pendingRegistration.setExpiryTime(LocalDateTime.now().plusMinutes(5));
        pendingRegistration.setSessionId(sessionId);
        pendingRegistrationRepository.save(pendingRegistration);

        // Generate OTP and register transaction synchronization for email dispatch
        otpService.generateAndSendOtp(user.getEmail(), OtpType.REGISTRATION);

        activityLogService.logSuccess(user.getEmail(), user.getRole() != null ? user.getRole().name() : "UNKNOWN", "REGISTER_START", "User registration initiated", null);

        return java.util.Map.of(
            "email", user.getEmail(),
            "role", user.getRole(),
            "sessionId", sessionId
        );
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User account was not found."));
        
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect.");
        }

        validatePassword(newPassword);
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        activityLogService.logSuccess(email, user.getRole().name(), "CHANGE_PASSWORD", "User successfully changed password", null);
    }

    @Transactional
    public void verifyEmail(String email, String otp, String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing session ID.");
        }
        
        if (otpService.verifyOtp(email, otp, OtpType.REGISTRATION)) {
            PendingRegistration pending = pendingRegistrationRepository.findByEmail(email)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration session expired or invalid."));

            if (!sessionId.equals(pending.getSessionId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session mismatch. Please try registering again.");
            }

            User user = new User();
            user.setEmail(pending.getEmail());
            user.setName(pending.getName());
            user.setPassword(pending.getPassword());
            user.setRole(pending.getRole());
            user.setEmailVerified(true);
            user.setAccountStatus(UserStatus.ACTIVE);
            
            userRepository.save(user);
            pendingRegistrationRepository.delete(pending);

            activityLogService.logSuccess(email, pending.getRole() != null ? pending.getRole().name() : "UNKNOWN", "EMAIL_VERIFIED", "User successfully verified email via OTP", null);
        }
    }

    @Transactional
    public java.util.Map<String, Object> resendRegistrationOtp(String email) {
        PendingRegistration pending = pendingRegistrationRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No active registration session found. Please register again."));
        
        // Extend the expiry time so the session doesn't expire before the new OTP does
        pending.setExpiryTime(LocalDateTime.now().plusMinutes(5));
        pendingRegistrationRepository.save(pending);

        // Generate and send new OTP (with its own 60s cooldown rules enforced by OtpService)
        otpService.generateAndSendOtp(email, OtpType.REGISTRATION);
        return java.util.Map.of(
                "message", "OTP resent successfully",
                "sessionId", pending.getSessionId(),
                "email", pending.getEmail()
        );
    }

    public java.util.Map<String, Object> getCurrentUserSummary(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User account was not found."));

        return java.util.Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole(),
                "emailVerified", user.isEmailVerified(),
                "accountStatus", user.getAccountStatus()
        );
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void cleanupExpiredPendingRegistrations() {
        pendingRegistrationRepository.deleteByExpiryTimeBefore(LocalDateTime.now());
        otpRepository.deleteByExpiryTimeBefore(LocalDateTime.now());
    }

    public void initiateForgotPassword(String email) {
        java.util.Optional<User> userOpt = userRepository.findByEmail(email);
        
        // If user doesn't exist, silently return to prevent email enumeration
        if (userOpt.isEmpty()) {
            logger.warn("Forgot password requested for non-existent email: {}", email);
            return;
        }

        // If user exists, generate actual OTP
        otpService.generateAndSendOtp(email, OtpType.FORGOT_PASSWORD);
    }

    @Transactional
    public void resetPassword(String email, String newPassword) {
        validatePassword(newPassword);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found."));
        
        // Consume the verified OTP first to prevent race condition re-use
        otpService.consumeVerifiedOtp(email, OtpType.FORGOT_PASSWORD);

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Forcefully revoke all active sessions for this user so they must log in again
        activeSessionRepository.deleteByUserId(user.getId());

        activityLogService.logSuccess(email, user.getRole() != null ? user.getRole().name() : "UNKNOWN", "RESET_PASSWORD", "User successfully reset password via OTP", null);
    }

}
