package com.legacyvault.service;

import com.legacyvault.dto.RoleContextDto;
import com.legacyvault.entity.Contact;
import com.legacyvault.entity.User;
import com.legacyvault.repository.ContactRepository;
import com.legacyvault.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final ContactRepository contactRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService) {
        this(userRepository, null, passwordEncoder, emailService);
    }

    @Autowired
    public AuthService(UserRepository userRepository,
                       ContactRepository contactRepository,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.contactRepository = contactRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public User register(String fullName, String email, String password) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name is required");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        String normalizedEmail = email.trim();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setFullName(fullName.trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole("vault_owner");
        user.setAccountStatus("ACTIVE");
        user.setIsVerified(false);

        String otp = generateOtp();
        user.setOtpCode(otp);
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(5));

        User savedUser = userRepository.save(user);
        emailService.sendOtpEmail(normalizedEmail, otp);
        return savedUser;
    }

    @Transactional
    public void verifyOtp(String email, String otpCode) {
        if (email == null || email.isBlank() || otpCode == null || otpCode.isBlank()) {
            throw new IllegalArgumentException("Email and OTP code are required");
        }

        User user = userRepository.findByEmail(email.trim())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (Boolean.TRUE.equals(user.getIsVerified())) {
            throw new RuntimeException("User already verified");
        }

        if (user.getOtpCode() == null || user.getOtpExpiresAt() == null) {
            throw new RuntimeException("Invalid OTP");
        }

        if (user.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }

        if (!user.getOtpCode().equals(otpCode.trim())) {
            throw new RuntimeException("Invalid OTP");
        }

        user.setIsVerified(true);
        user.setOtpCode(null);
        user.setOtpExpiresAt(null);
        userRepository.save(user);
    }

    @Transactional
    public User resendOtp(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        User user = userRepository.findByEmail(email.trim())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (Boolean.TRUE.equals(user.getIsVerified())) {
            throw new RuntimeException("User already verified");
        }

        String otp = generateOtp();
        user.setOtpCode(otp);
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(5));
        User saved = userRepository.save(user);
        emailService.sendOtpEmail(user.getEmail(), otp);
        return saved;
    }

    @Transactional
    public User login(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Email and password are required");
        }

        User user = userRepository.findByEmail(email.trim())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (!Boolean.TRUE.equals(user.getIsVerified())) {
            throw new RuntimeException("User not verified. Please verify OTP first.");
        }

        if ("SUSPENDED".equalsIgnoreCase(user.getAccountStatus())) {
            throw new RuntimeException("Account is suspended. Contact support for assistance.");
        }

        if ("DEACTIVATED".equalsIgnoreCase(user.getAccountStatus())) {
            throw new RuntimeException("Account is deactivated. Contact support for assistance.");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        return user;
    }

    @Transactional(readOnly = true)
    public User getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional(readOnly = true)
    public List<RoleContextDto> getAvailableContexts(User user) {
        List<RoleContextDto> contexts = new ArrayList<>();
        if (user == null) {
            return contexts;
        }

        String primaryRole = (user.getRole() != null && !user.getRole().isBlank())
                ? user.getRole().trim().toLowerCase(Locale.ROOT)
                : "vault_owner";

        String primaryLabel = switch (primaryRole) {
            case "admin", "system_admin" -> "System Admin";
            case "legal_verifier" -> "Legal Verifier";
            default -> "Vault Owner";
        };

        contexts.add(new RoleContextDto(
                primaryRole,
                "SYSTEM",
                primaryLabel,
                null,
                user.getId(),
                user.getFullName()
        ));

        if (contactRepository != null && user.getEmail() != null && !user.getEmail().isBlank()) {
            List<Contact> verifiedLinks = contactRepository.findByEmailIgnoreCaseAndStatus(user.getEmail().trim(), "verified");
            if (verifiedLinks != null) {
                for (Contact c : verifiedLinks) {
                    if (user.getId() != null && user.getId().equals(c.getOwnerId())) {
                        continue; // Owner cannot invite themselves
                    }
                    String ownerName = userRepository.findById(c.getOwnerId())
                            .map(User::getFullName)
                            .orElse("Vault Owner");

                    if (Boolean.TRUE.equals(c.getIsExecutor())) {
                        contexts.add(new RoleContextDto(
                                "digital_executor",
                                "VAULT",
                                "Executor Dashboard",
                                "Executor for " + ownerName,
                                c.getOwnerId(),
                                ownerName
                        ));
                    }
                    if (Boolean.TRUE.equals(c.getIsBeneficiary())) {
                        contexts.add(new RoleContextDto(
                                "beneficiary",
                                "VAULT",
                                "Beneficiary Dashboard",
                                "Beneficiary for " + ownerName,
                                c.getOwnerId(),
                                ownerName
                        ));
                    }
                }
            }
        }

        return contexts;
    }

    private String generateOtp() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }
}
