package com.legacyvault.service;

import com.legacyvault.entity.User;
import com.legacyvault.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("test-uuid");
        testUser.setFullName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("encoded_password");
        testUser.setOtpCode("123456");
        testUser.setOtpExpiresAt(LocalDateTime.now().plusMinutes(5));
        testUser.setIsVerified(false);
    }

    @Test
    void testRegister_Success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User savedUser = authService.register("Test User", "test@example.com", "password");

        assertNotNull(savedUser);
        assertEquals("test@example.com", savedUser.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
        verify(emailService, times(1)).sendOtpEmail(eq("test@example.com"), anyString());
    }

    @Test
    void testRegister_EmailExists() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        Exception exception = assertThrows(RuntimeException.class, () -> 
            authService.register("Test User", "test@example.com", "password")
        );
        assertEquals("Email already exists", exception.getMessage());
    }

    @Test
    void testVerifyOtp_Success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        
        assertDoesNotThrow(() -> authService.verifyOtp("test@example.com", "123456"));
        
        assertTrue(testUser.getIsVerified());
        assertNull(testUser.getOtpCode());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void testVerifyOtp_InvalidOtp() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        
        Exception exception = assertThrows(RuntimeException.class, () -> 
            authService.verifyOtp("test@example.com", "wrong_otp")
        );
        assertEquals("Invalid OTP", exception.getMessage());
    }

    @Test
    void testLogin_SuspendedAccountIsRejected() {
        testUser.setIsVerified(true);
        testUser.setAccountStatus("SUSPENDED");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password", "encoded_password")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.login("test@example.com", "password"));

        assertEquals("Account is suspended. Contact support for assistance.", exception.getMessage());
    }
}
