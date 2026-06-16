package com.aihiringplatform.backend.service;

import com.aihiringplatform.backend.dto.AuthRequest;
import com.aihiringplatform.backend.dto.AuthResponse;
import com.aihiringplatform.backend.entity.Role;
import com.aihiringplatform.backend.entity.User;
import com.aihiringplatform.backend.repository.UserRepository;
import com.aihiringplatform.backend.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private com.aihiringplatform.backend.repository.ActiveSessionRepository activeSessionRepository;

    @InjectMocks
    private AuthService authService;

    private User verifiedUser;
    private User unverifiedUser;
    private AuthRequest validRequest;

    @BeforeEach
    void setUp() {
        verifiedUser = new User();
        verifiedUser.setEmail("verified@test.com");
        verifiedUser.setPassword("hashedPassword");
        verifiedUser.setRole(Role.CANDIDATE);
        verifiedUser.setEmailVerified(true);

        unverifiedUser = new User();
        unverifiedUser.setEmail("unverified@test.com");
        unverifiedUser.setPassword("hashedPassword");
        unverifiedUser.setRole(Role.CANDIDATE);
        unverifiedUser.setEmailVerified(false);

        validRequest = new AuthRequest();
        validRequest.setEmail("verified@test.com");
        validRequest.setPassword("Password123!");
    }

    @Test
    void login_VerifiedUser_ShouldReturnToken() {
        when(userRepository.findByEmail("verified@test.com")).thenReturn(Optional.of(verifiedUser));
        when(passwordEncoder.matches("Password123!", "hashedPassword")).thenReturn(true);
        when(jwtUtil.generateToken("verified@test.com")).thenReturn("mock-jwt-token");

        AuthResponse response = authService.login(validRequest, org.mockito.Mockito.mock(jakarta.servlet.http.HttpServletRequest.class));

        assertNotNull(response);
        assertEquals("mock-jwt-token", response.getToken());
        assertEquals("CANDIDATE", response.getRole());
    }

    @Test
    void login_UnverifiedUser_ShouldThrowForbiddenException() {
        validRequest.setEmail("unverified@test.com");
        when(userRepository.findByEmail("unverified@test.com")).thenReturn(Optional.of(unverifiedUser));
        when(passwordEncoder.matches("Password123!", "hashedPassword")).thenReturn(true);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            authService.login(validRequest, org.mockito.Mockito.mock(jakarta.servlet.http.HttpServletRequest.class));
        });

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Please verify your email address before logging in.", exception.getReason());
    }
}
