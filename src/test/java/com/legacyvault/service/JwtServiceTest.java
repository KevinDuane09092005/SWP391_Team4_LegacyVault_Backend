package com.legacyvault.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

public class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "expiration", 3600000L);
    }

    @Test
    void testGenerateAndValidateToken_Success() {
        String token = jwtService.generateToken("user-123", "owner@example.com", "vault_owner");

        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
        assertEquals("user-123", jwtService.extractUserId(token));
        assertEquals("owner@example.com", jwtService.extractEmail(token));
        assertEquals("vault_owner", jwtService.extractRole(token));
    }

    @Test
    void testValidateToken_InvalidToken() {
        assertFalse(jwtService.validateToken("invalid.jwt.token"));
    }
}
