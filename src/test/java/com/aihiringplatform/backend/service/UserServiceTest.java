package com.aihiringplatform.backend.service;

import com.aihiringplatform.backend.entity.Role;
import com.aihiringplatform.backend.entity.User;
import com.aihiringplatform.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.aihiringplatform.backend.repository.PendingRegistrationRepository pendingRegistrationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OtpService otpService;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private com.aihiringplatform.backend.repository.ActiveSessionRepository activeSessionRepository;

    @InjectMocks
    private UserService userService;

    private User newCandidate;
    private User newRecruiter;

    @BeforeEach
    void setUp() {
        newCandidate = new User();
        newCandidate.setEmail("candidate@test.com");
        newCandidate.setPassword("Password123!");
        newCandidate.setRole(Role.CANDIDATE);
        newCandidate.setName("Candidate User");

        newRecruiter = new User();
        newRecruiter.setEmail("recruiter@test.com");
        newRecruiter.setPassword("Password123!");
        newRecruiter.setRole(Role.RECRUITER);
        newRecruiter.setName("Recruiter User");
    }

    @Test
    void registerUser_WhenEmailIsUnique_ShouldSaveUser() {
        when(userRepository.findByEmail(anyString())).thenReturn(java.util.Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(pendingRegistrationRepository.findByEmail(anyString())).thenReturn(java.util.Optional.empty());
        when(pendingRegistrationRepository.save(any(com.aihiringplatform.backend.entity.PendingRegistration.class))).thenReturn(new com.aihiringplatform.backend.entity.PendingRegistration());
        doNothing().when(otpService).generateAndSendOtp(anyString(), any());

        java.util.Map<String, Object> savedUser = userService.registerUser(newCandidate);

        assertNotNull(savedUser);
        verify(pendingRegistrationRepository).save(any(com.aihiringplatform.backend.entity.PendingRegistration.class));
        verify(otpService).generateAndSendOtp(eq("candidate@test.com"), any());
    }

    @Test
    void registerUser_CandidateDuplicateEmail_ShouldThrowConflictException() {
        User existingUser = new User();
        existingUser.setEmailVerified(true);
        when(userRepository.findByEmail("candidate@test.com")).thenReturn(java.util.Optional.of(existingUser));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.registerUser(newCandidate);
        });

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Account already exists.", exception.getReason());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_RecruiterDuplicateEmail_ShouldThrowConflictException() {
        User existingUser = new User();
        existingUser.setEmailVerified(true);
        when(userRepository.findByEmail("recruiter@test.com")).thenReturn(java.util.Optional.of(existingUser));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            userService.registerUser(newRecruiter);
        });

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Account already exists.", exception.getReason());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_UnverifiedDuplicateEmail_ShouldOverwriteAndResendOtp() {
        com.aihiringplatform.backend.entity.PendingRegistration existingPending = new com.aihiringplatform.backend.entity.PendingRegistration();
        existingPending.setEmail("candidate@test.com");
        
        when(userRepository.findByEmail("candidate@test.com")).thenReturn(java.util.Optional.empty());
        when(pendingRegistrationRepository.findByEmail("candidate@test.com")).thenReturn(java.util.Optional.of(existingPending));
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(pendingRegistrationRepository.save(any(com.aihiringplatform.backend.entity.PendingRegistration.class))).thenReturn(existingPending);
        doNothing().when(otpService).generateAndSendOtp(anyString(), any());

        java.util.Map<String, Object> savedUser = userService.registerUser(newCandidate);

        assertNotNull(savedUser);
        verify(pendingRegistrationRepository).save(any(com.aihiringplatform.backend.entity.PendingRegistration.class));
        verify(otpService).generateAndSendOtp(eq("candidate@test.com"), any());
    }

    @Test
    void initiateForgotPassword_UserExists_ShouldGenerateOtp() {
        when(userRepository.findByEmail("candidate@test.com")).thenReturn(java.util.Optional.of(newCandidate));
        doNothing().when(otpService).generateAndSendOtp("candidate@test.com", com.aihiringplatform.backend.entity.OtpType.FORGOT_PASSWORD);

        userService.initiateForgotPassword("candidate@test.com");

        verify(otpService).generateAndSendOtp("candidate@test.com", com.aihiringplatform.backend.entity.OtpType.FORGOT_PASSWORD);
    }

    @Test
    void initiateForgotPassword_UserDoesNotExist_ShouldNotGenerateOtp() {
        when(userRepository.findByEmail("nonexistent@test.com")).thenReturn(java.util.Optional.empty());

        userService.initiateForgotPassword("nonexistent@test.com");

        verify(otpService, never()).generateAndSendOtp(anyString(), any());
    }

    @Test
    void resetPassword_ValidOtp_ShouldUpdatePassword() {
        when(otpService.consumeVerifiedOtp("candidate@test.com", com.aihiringplatform.backend.entity.OtpType.FORGOT_PASSWORD)).thenReturn(true);
        when(userRepository.findByEmail("candidate@test.com")).thenReturn(java.util.Optional.of(newCandidate));
        when(passwordEncoder.encode("NewStrongPass1!")).thenReturn("hashedNewPass");

        userService.resetPassword("candidate@test.com", "NewStrongPass1!");

        verify(userRepository).save(newCandidate);
        assertEquals("hashedNewPass", newCandidate.getPassword());
    }
}
