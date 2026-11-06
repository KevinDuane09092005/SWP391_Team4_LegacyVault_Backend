package com.legacyvault.controller;

import com.legacyvault.dto.RoleContextDto;
import com.legacyvault.entity.User;
import com.legacyvault.service.AuthService;
import com.legacyvault.service.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        try {
            String fullName = firstNonBlank(request.get("fullName"), request.get("full_name"));
            String email = request.get("email");
            String password = request.get("password");

            User user = authService.register(fullName, email, password);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("message", "OTP sent successfully");
            body.put("userId", user.getId());
            body.put("email", user.getEmail());
            body.put("otpExpiresAt", user.getOtpExpiresAt());
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return mapAuthError(e);
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String otpCode = firstNonBlank(request.get("otpCode"), request.get("otp_code"));
            authService.verifyOtp(email, otpCode);
            return ResponseEntity.ok(Map.of(
                    "message", "Verified successfully",
                    "isVerified", true
            ));
        } catch (Exception e) {
            return mapAuthError(e);
        }
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody Map<String, String> request) {
        try {
            User user = authService.resendOtp(request.get("email"));
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("message", "OTP resent successfully");
            body.put("userId", user.getId());
            body.put("email", user.getEmail());
            body.put("otpExpiresAt", user.getOtpExpiresAt());
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return mapAuthError(e);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        try {
            User user = authService.login(request.get("email"), request.get("password"));
            String role = (user.getRole() != null && !user.getRole().isBlank()) ? user.getRole() : "vault_owner";
            String token = jwtService.generateToken(user.getId(), user.getEmail(), role);
            List<RoleContextDto> availableContexts = authService.getAvailableContexts(user);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("message", "Login successful");
            body.put("userId", user.getId());
            body.put("fullName", user.getFullName());
            body.put("email", user.getEmail());
            body.put("role", role);
            body.put("token", token);
            body.put("availableContexts", availableContexts);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return mapAuthError(e);
        }
    }

    @GetMapping("/contexts")
    public ResponseEntity<?> getContexts(Authentication authentication) {
        try {
            if (authentication == null || authentication.getName() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Unauthorized: No user session found"));
            }
            User user = authService.getUserById(authentication.getName());
            String role = (user.getRole() != null && !user.getRole().isBlank()) ? user.getRole() : "vault_owner";
            List<RoleContextDto> availableContexts = authService.getAvailableContexts(user);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("userId", user.getId());
            body.put("fullName", user.getFullName());
            body.put("email", user.getEmail());
            body.put("role", role);
            body.put("availableContexts", availableContexts);
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return mapAuthError(e);
        }
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }

    private ResponseEntity<Map<String, String>> mapAuthError(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage() : "Authentication error";
        String lower = msg.toLowerCase(Locale.ROOT);
        HttpStatus status = HttpStatus.BAD_REQUEST;

        if (lower.contains("already exists")) {
            status = HttpStatus.CONFLICT;
        } else if (lower.contains("invalid credentials")) {
            status = HttpStatus.UNAUTHORIZED;
        } else if (lower.contains("not verified") || lower.contains("suspended") || lower.contains("deactivated")) {
            status = HttpStatus.FORBIDDEN;
        } else if (lower.contains("not found")) {
            status = HttpStatus.NOT_FOUND;
        }

        return ResponseEntity.status(status).body(Map.of("error", msg));
    }
}
