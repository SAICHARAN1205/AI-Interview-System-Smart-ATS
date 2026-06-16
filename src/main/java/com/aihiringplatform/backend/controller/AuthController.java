package com.aihiringplatform.backend.controller;

import com.aihiringplatform.backend.dto.AuthRequest;
import com.aihiringplatform.backend.dto.AuthResponse;
import com.aihiringplatform.backend.service.AuthService;
import com.aihiringplatform.backend.service.CaptchaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.aihiringplatform.backend.service.UserService;
import com.aihiringplatform.backend.service.OtpService;
import com.aihiringplatform.backend.entity.OtpType;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private CaptchaService captchaService;

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        captchaService.validateCaptcha(request.getCaptchaToken(), request.getCaptchaAnswer());
        return authService.login(request, httpRequest);
    }

    @Autowired
    private UserService userService;

    @Autowired
    private OtpService otpService;

    @PostMapping("/verify-registration-otp")
    public ResponseEntity<?> verifyRegistrationOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");
        String sessionId = request.get("sessionId");
        userService.verifyEmail(email, otp, sessionId);
        return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
    }

    @PostMapping("/resend-registration-otp")
    public ResponseEntity<?> resendRegistrationOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        return ResponseEntity.ok(userService.resendRegistrationOtp(email));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        userService.initiateForgotPassword(email);
        return ResponseEntity.ok(Map.of("message", "If the account exists, an OTP has been sent to your email."));
    }

    @PostMapping("/verify-reset-otp")
    public ResponseEntity<?> verifyResetOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");
        otpService.verifyOtp(email, otp, OtpType.FORGOT_PASSWORD);
        return ResponseEntity.ok(Map.of("message", "OTP verified successfully. You may now reset your password."));
    }

    @PostMapping("/refresh")
    public AuthResponse refreshSession(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        return authService.refreshSession(request.get("refreshToken"), httpRequest);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String newPassword = request.get("newPassword");
        userService.resetPassword(email, newPassword);
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }
}
