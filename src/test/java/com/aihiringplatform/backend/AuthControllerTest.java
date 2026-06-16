package com.aihiringplatform.backend;

import com.aihiringplatform.backend.dto.AuthRequest;
import com.aihiringplatform.backend.dto.AuthResponse;
import com.aihiringplatform.backend.entity.Role;
import com.aihiringplatform.backend.entity.User;
import com.aihiringplatform.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.aihiringplatform.backend.service.AuthService;
import com.aihiringplatform.backend.service.CaptchaService;
import com.aihiringplatform.backend.service.UserService;
import com.aihiringplatform.backend.service.OtpService;
import com.aihiringplatform.backend.entity.OtpType;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private AuthService authService;

    @MockBean
    private CaptchaService captchaService;

    @MockBean
    private UserService userService;

    @MockBean
    private OtpService otpService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testLogin_Success() throws Exception {
        // Given
        AuthRequest request = new AuthRequest();
        request.setEmail("john@example.com");
        request.setPassword("Password123!");

        AuthResponse response = new AuthResponse();
        response.setToken("jwt-token-here");

        when(authService.login(any(AuthRequest.class), any())).thenReturn(response);
        doNothing().when(captchaService).validateCaptcha(any(), any());

        // When & Then
        // GlobalResponseHandler wraps AuthResponse in ApiResponse{success, message, data}
        // AuthResponse is a String-mapped type so check $.data.token
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("jwt-token-here"));
    }

    @Test
    public void testLogin_InvalidCredentials() throws Exception {
        // Given
        AuthRequest request = new AuthRequest();
        request.setEmail("john@example.com");
        request.setPassword("wrongpassword");

        // Use ResponseStatusException so GlobalExceptionHandler maps it to 400, not 500
        when(authService.login(any(AuthRequest.class), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid credentials"));
        doNothing().when(captchaService).validateCaptcha(any(), any());

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testLogin_InvalidInput() throws Exception {
        // Given
        AuthRequest request = new AuthRequest();
        request.setEmail(""); // Invalid: blank email
        request.setPassword("");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testLogin_InvalidCaptcha() throws Exception {
        // Given
        AuthRequest request = new AuthRequest();
        request.setEmail("john@example.com");
        request.setPassword("Password123!");
        request.setCaptchaToken("invalid-token");
        request.setCaptchaAnswer("wrong");

        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Incorrect CAPTCHA. Please try again."))
                .when(captchaService).validateCaptcha(any(), any());

        // When & Then — GlobalResponseHandler wraps error as $.message
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Incorrect CAPTCHA. Please try again."));
    }

    @Test
    public void testForgotPassword_ShouldReturnSuccess() throws Exception {
        java.util.Map<String, String> request = new java.util.HashMap<>();
        request.put("email", "john@example.com");

        doNothing().when(userService).initiateForgotPassword("john@example.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("If the account exists, an OTP has been sent to your email."));
    }

    @Test
    public void testVerifyResetOtp_ShouldReturnSuccess() throws Exception {
        java.util.Map<String, String> request = new java.util.HashMap<>();
        request.put("email", "john@example.com");
        request.put("otp", "123456");

        when(otpService.verifyOtp("john@example.com", "123456", OtpType.FORGOT_PASSWORD)).thenReturn(true);

        mockMvc.perform(post("/api/auth/verify-reset-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("OTP verified successfully. You may now reset your password."));
    }

    @Test
    public void testResetPassword_ShouldReturnSuccess() throws Exception {
        java.util.Map<String, String> request = new java.util.HashMap<>();
        request.put("email", "john@example.com");
        request.put("newPassword", "NewPassword123!");

        doNothing().when(userService).resetPassword("john@example.com", "NewPassword123!");

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Password reset successfully"));
    }
}
