package com.legacyvault.service;

import com.legacyvault.entity.User;
import com.legacyvault.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }
    
    @Transactional
    public User register(String fullName, String email, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }
        
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        
        // Generate OTP
        String otp = generateOtp();
        user.setOtpCode(otp);
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(5));
        
        User savedUser = userRepository.save(user);
        emailService.sendOtpEmail(email, otp);
        return savedUser;
    }
    
    @Transactional
    public void verifyOtp(String email, String otpCode) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        if (user.getIsVerified()) {
            throw new RuntimeException("User already verified");
        }
        
        if (user.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }
        
        if (!user.getOtpCode().equals(otpCode)) {
            throw new RuntimeException("Invalid OTP");
        }
        
        user.setIsVerified(true);
        user.setOtpCode(null);
        user.setOtpExpiresAt(null);
        userRepository.save(user);
    }
    
    public User login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
                
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new RuntimeException("User account is suspended");
        }
        
        if (!user.getIsVerified()) {
            throw new RuntimeException("User not verified. Please verify OTP first.");
        }
        
        return user;
    }
    
    private String generateOtp() {
        return String.format("%06d", new Random().nextInt(999999));
    }
}
