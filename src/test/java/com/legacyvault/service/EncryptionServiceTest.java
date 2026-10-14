package com.legacyvault.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
    }

    @Test
    void testEncryptAndDecrypt_Success() {
        String plainText = "MySuperSecretBankPin1234!";
        String cipherText = encryptionService.encrypt(plainText);

        assertNotNull(cipherText);
        assertNotEquals(plainText, cipherText);

        String decrypted = encryptionService.decrypt(cipherText);
        assertEquals(plainText, decrypted);
    }

    @Test
    void testEncrypt_NullOrEmpty() {
        assertNull(encryptionService.encrypt(null));
        assertEquals("", encryptionService.encrypt(""));
    }

    @Test
    void testDecrypt_NullOrEmpty() {
        assertNull(encryptionService.decrypt(null));
        assertEquals("", encryptionService.decrypt(""));
    }

    @Test
    void testDecrypt_InvalidDataThrows() {
        assertThrows(RuntimeException.class, () -> encryptionService.decrypt("invalid-base64-data"));
    }
}
