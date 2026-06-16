package com.aihiringplatform.backend.service;

import com.aihiringplatform.backend.dto.AuthRequest;
import com.aihiringplatform.backend.dto.AuthResponse;
import com.aihiringplatform.backend.entity.User;
import com.aihiringplatform.backend.repository.UserRepository;
import com.aihiringplatform.backend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import jakarta.servlet.http.HttpServletRequest;
import com.aihiringplatform.backend.entity.ActiveSession;
import com.aihiringplatform.backend.repository.ActiveSessionRepository;
import java.time.LocalDateTime;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private ActivityLogService activityLogService;

    @Autowired
    private ActiveSessionRepository activeSessionRepository;

    public AuthResponse login(AuthRequest request, HttpServletRequest httpRequest) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    activityLogService.logFailure(request.getEmail(), "UNKNOWN", "LOGIN", "Invalid email", httpRequest);
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
                });

        if (user.getAccountLockedUntil() != null && user.getAccountLockedUntil().isAfter(LocalDateTime.now())) {
            activityLogService.logFailure(user.getEmail(), user.getRole().name(), "LOGIN", "Account is temporarily locked", httpRequest);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many failed attempts. Try again later.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            int newFailures = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(newFailures);
            if (newFailures >= 5) {
                user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(15));
                activityLogService.logSecurityEvent(user.getEmail(), "ACCOUNT_LOCKED", "Locked due to 5 failed attempts", httpRequest);
            } else {
                activityLogService.logFailure(user.getEmail(), user.getRole().name(), "LOGIN", "Invalid password. Attempt " + newFailures, httpRequest);
            }
            userRepository.save(user);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        if (!user.isEmailVerified()) {
            activityLogService.logFailure(user.getEmail(), user.getRole().name(), "LOGIN", "Unverified email", httpRequest);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Please verify your email address before logging in.");
        }

        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.save(user);
        activityLogService.logSuccess(user.getEmail(), user.getRole().name(), "LOGIN", "Successful login", httpRequest);

        String token = jwtUtil.generateToken(user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken();

        ActiveSession session = new ActiveSession();
        session.setUser(user);
        session.setRefreshToken(refreshToken);
        session.setDeviceInfo(httpRequest.getHeader("User-Agent"));
        session.setIpAddress(getClientIp(httpRequest));
        session.setExpiresAt(LocalDateTime.now().plusDays(7));
        activeSessionRepository.save(session);

        return new AuthResponse(token, refreshToken, user.getRole() != null ? user.getRole().name() : "CANDIDATE");
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    public AuthResponse refreshSession(String refreshToken, HttpServletRequest request) {
        if (refreshToken == null || refreshToken.isBlank()) {
            activityLogService.logFailure("Unknown", "UNKNOWN", "TOKEN_REFRESH", "Refresh token missing", request);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token missing");
        }

        ActiveSession session = activeSessionRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> {
                    activityLogService.logFailure("Unknown", "UNKNOWN", "TOKEN_REFRESH", "Invalid refresh token", request);
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
                });

        User user = session.getUser();

        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            activeSessionRepository.delete(session);
            activityLogService.logFailure(user.getEmail(), user.getRole().name(), "TOKEN_REFRESH", "Refresh token expired", request);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }

        String newToken = jwtUtil.generateToken(user.getEmail());
        String newRefreshToken = jwtUtil.generateRefreshToken();

        session.setRefreshToken(newRefreshToken);
        session.setExpiresAt(LocalDateTime.now().plusDays(7));
        session.setIpAddress(getClientIp(request));
        activeSessionRepository.save(session);

        activityLogService.logSuccess(user.getEmail(), user.getRole().name(), "TOKEN_REFRESH", "Session refreshed successfully", request);

        return new AuthResponse(newToken, newRefreshToken, user.getRole() != null ? user.getRole().name() : "CANDIDATE");
    }
}
