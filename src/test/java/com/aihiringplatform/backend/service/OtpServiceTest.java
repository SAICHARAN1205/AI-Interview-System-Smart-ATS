package com.aihiringplatform.backend.service;

import com.aihiringplatform.backend.entity.OtpToken;
import com.aihiringplatform.backend.entity.OtpType;
import com.aihiringplatform.backend.repository.OtpRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OtpServiceTest {

    @Mock
    private OtpRepository otpRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private OtpService otpService;

    private OtpToken validToken;
    private OtpToken expiredToken;

    @BeforeEach
    void setUp() {
        validToken = new OtpToken();
        validToken.setEmail("test@test.com");
        validToken.setOtpCode("hashedOtp");
        validToken.setExpiryTime(LocalDateTime.now().plusMinutes(5));
        validToken.setType(OtpType.REGISTRATION);
        validToken.setVerified(false);
        validToken.setAttempts(0);
        validToken.setCreatedAt(LocalDateTime.now().minusMinutes(2));

        expiredToken = new OtpToken();
        expiredToken.setEmail("test@test.com");
        expiredToken.setOtpCode("hashedOtp");
        expiredToken.setExpiryTime(LocalDateTime.now().minusMinutes(5));
        expiredToken.setType(OtpType.REGISTRATION);
        expiredToken.setVerified(false);
        expiredToken.setAttempts(0);
    }

    @Test
    void verifyOtp_ValidOtp_ShouldActivateAccount() {
        when(otpRepository.findTopByEmailAndTypeOrderByExpiryTimeDesc("test@test.com", OtpType.REGISTRATION))
                .thenReturn(Optional.of(validToken));
        when(passwordEncoder.matches("123456", "hashedOtp")).thenReturn(true);

        boolean result = otpService.verifyOtp("test@test.com", "123456", OtpType.REGISTRATION);

        assertTrue(result);
        assertTrue(validToken.isVerified());
        verify(otpRepository).save(validToken);
    }

    @Test
    void verifyOtp_ExpiredOtp_ShouldThrowBadRequest() {
        when(otpRepository.findTopByEmailAndTypeOrderByExpiryTimeDesc("test@test.com", OtpType.REGISTRATION))
                .thenReturn(Optional.of(expiredToken));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            otpService.verifyOtp("test@test.com", "123456", OtpType.REGISTRATION);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Verification code expired.", exception.getReason());
    }

    @Test
    void generateAndSendOtp_ResendOtp_ShouldInvalidateOldAndSendNew() {
        when(otpRepository.findTopByEmailAndTypeOrderByExpiryTimeDesc("test@test.com", OtpType.REGISTRATION))
                .thenReturn(Optional.of(validToken));
        when(passwordEncoder.encode(anyString())).thenReturn("newHashedOtp");

        otpService.generateAndSendOtp("test@test.com", OtpType.REGISTRATION);

        // The old token should have its expiry set to past
        assertTrue(validToken.getExpiryTime().isBefore(LocalDateTime.now()));
        
        // Save should be called twice: once for old invalidation, once for new token
        verify(otpRepository, times(2)).save(any(OtpToken.class));
        verify(emailService).sendRegistrationOtp(eq("test@test.com"), anyString());
    }
}
