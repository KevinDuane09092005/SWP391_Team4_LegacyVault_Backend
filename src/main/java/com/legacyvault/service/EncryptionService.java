package com.legacyvault.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Service
public class EncryptionService {

    public static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int IV_SIZE = 16;
    private static final int KEY_SIZE_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final byte[] defaultKeyBytes;

    @Autowired
    public EncryptionService() {
        this.defaultKeyBytes = null;
    }

    public EncryptionService(String fallbackSecretKey) {
        this.defaultKeyBytes = (fallbackSecretKey != null && !fallbackSecretKey.isBlank())
                ? deriveKeyBytes(fallbackSecretKey)
                : null;
    }

    /**
     * Generates a cryptographically random 256-bit (32-byte) AES key encoded as a 64-character hex string.
     */
    public String generateRandomAesKey() {
        byte[] randomBytes = new byte[KEY_SIZE_BYTES];
        SECURE_RANDOM.nextBytes(randomBytes);
        StringBuilder sb = new StringBuilder(KEY_SIZE_BYTES * 2);
        for (byte b : randomBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public String encryptWithKey(String plainText, String assetAesKey) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        byte[] keyBytes = deriveKeyBytes(assetAesKey);
        byte[] encrypted = encryptBytesInternal(plainText.getBytes(StandardCharsets.UTF_8), keyBytes);
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public String decryptWithKey(String cipherText, String assetAesKey) {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(cipherText);
            byte[] keyBytes = deriveKeyBytes(assetAesKey);
            byte[] decrypted = decryptBytesInternal(combined, keyBytes);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    public byte[] encryptBytesWithKey(byte[] plainBytes, String assetAesKey) {
        if (plainBytes == null || plainBytes.length == 0) {
            return plainBytes;
        }
        byte[] keyBytes = deriveKeyBytes(assetAesKey);
        return encryptBytesInternal(plainBytes, keyBytes);
    }

    public byte[] decryptBytesWithKey(byte[] cipherBytes, String assetAesKey) {
        if (cipherBytes == null || cipherBytes.length == 0) {
            return cipherBytes;
        }
        byte[] keyBytes = deriveKeyBytes(assetAesKey);
        return decryptBytesInternal(cipherBytes, keyBytes);
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        byte[] encrypted = encryptBytesInternal(plainText.getBytes(StandardCharsets.UTF_8), requireDefaultKey());
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(cipherText);
            byte[] decrypted = decryptBytesInternal(combined, requireDefaultKey());
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    public byte[] encryptBytes(byte[] plainBytes) {
        if (plainBytes == null || plainBytes.length == 0) {
            return plainBytes;
        }
        return encryptBytesInternal(plainBytes, requireDefaultKey());
    }

    public byte[] decryptBytes(byte[] cipherBytes) {
        if (cipherBytes == null || cipherBytes.length == 0) {
            return cipherBytes;
        }
        return decryptBytesInternal(cipherBytes, requireDefaultKey());
    }

    private byte[] requireDefaultKey() {
        if (this.defaultKeyBytes == null) {
            throw new IllegalStateException("Strict per-asset key mode is active: an asset-specific AES key from asset_keys is required");
        }
        return this.defaultKeyBytes;
    }

    private static byte[] deriveKeyBytes(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            throw new IllegalArgumentException("Asset AES key must not be null or blank");
        }
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return sha.digest(rawKey.trim().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize AES key", e);
        }
    }

    private static byte[] encryptBytesInternal(byte[] plainBytes, byte[] keyBytes) {
        try {
            byte[] iv = new byte[IV_SIZE];
            SECURE_RANDOM.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

            byte[] encrypted = cipher.doFinal(plainBytes);

            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return combined;
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    private static byte[] decryptBytesInternal(byte[] cipherBytes, byte[] keyBytes) {
        try {
            if (cipherBytes.length <= IV_SIZE) {
                throw new IllegalArgumentException("Invalid encrypted data length");
            }

            byte[] iv = Arrays.copyOfRange(cipherBytes, 0, IV_SIZE);
            byte[] encryptedPayload = Arrays.copyOfRange(cipherBytes, IV_SIZE, cipherBytes.length);

            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            return cipher.doFinal(encryptedPayload);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
