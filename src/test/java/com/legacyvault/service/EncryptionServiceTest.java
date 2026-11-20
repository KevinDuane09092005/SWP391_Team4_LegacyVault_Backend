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

    @Test
    void testEncryptAndDecryptBytes_Success() {
        byte[] plainBytes = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x37, 0x0A, (byte) 0xFF, 0x00, 0x42};
        byte[] encryptedBytes = encryptionService.encryptBytes(plainBytes);

        assertNotNull(encryptedBytes);
        assertTrue(encryptedBytes.length > plainBytes.length);

        byte[] decryptedBytes = encryptionService.decryptBytes(encryptedBytes);
        assertArrayEquals(plainBytes, decryptedBytes);
    }

    @Test
    void testEncryptAndDecryptBytes_NullOrEmpty() {
        assertNull(encryptionService.encryptBytes(null));
        assertArrayEquals(new byte[0], encryptionService.encryptBytes(new byte[0]));
        assertNull(encryptionService.decryptBytes(null));
        assertArrayEquals(new byte[0], encryptionService.decryptBytes(new byte[0]));
    }

    @Test
    void testPerAssetKeyGenerationAndIsolation() {
        EncryptionService strictService = new EncryptionService();
        String keyAsset1 = strictService.generateRandomAesKey();
        String keyAsset2 = strictService.generateRandomAesKey();

        assertNotNull(keyAsset1);
        assertNotNull(keyAsset2);
        assertEquals(64, keyAsset1.length());
        assertEquals(64, keyAsset2.length());
        assertNotEquals(keyAsset1, keyAsset2);

        String cipher1 = strictService.encryptWithKey("SecretForAsset1", keyAsset1);
        assertEquals("SecretForAsset1", strictService.decryptWithKey(cipher1, keyAsset1));
        assertThrows(RuntimeException.class, () -> strictService.decryptWithKey(cipher1, keyAsset2));

        byte[] rawDoc = "LegalPdfContent".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] encDoc = strictService.encryptBytesWithKey(rawDoc, keyAsset1);
        assertArrayEquals(rawDoc, strictService.decryptBytesWithKey(encDoc, keyAsset1));
        assertThrows(RuntimeException.class, () -> strictService.decryptBytesWithKey(encDoc, keyAsset2));
    }
}
