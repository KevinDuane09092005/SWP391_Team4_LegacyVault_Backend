package com.legacyvault.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Service
public class FaceEmbeddingEncryptionService {

    private static final int KEY_LENGTH_BYTES = 32;
    private static final int NONCE_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final byte FORMAT_VERSION = 1;
    private static final int MIN_EMBEDDING_LENGTH = 64;
    private static final int MAX_EMBEDDING_LENGTH = 4096;

    private final String encodedKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public FaceEmbeddingEncryptionService(@Value("${FACE_ENCRYPTION_KEY:}") String encodedKey) {
        this.encodedKey = encodedKey;
    }

    public byte[] encrypt(double[] embedding) {
        validateLength(embedding == null ? -1 : embedding.length);
        ByteBuffer plaintext = ByteBuffer.allocate(Integer.BYTES + embedding.length * Double.BYTES);
        plaintext.putInt(embedding.length);
        for (double value : embedding) {
            if (!Double.isFinite(value) || Math.abs(value) > 10) {
                throw new IllegalArgumentException("Face embedding contains an invalid value");
            }
            plaintext.putDouble(value);
        }

        byte[] nonce = new byte[NONCE_LENGTH_BYTES];
        secureRandom.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            byte[] ciphertext = cipher.doFinal(plaintext.array());
            byte[] encrypted = new byte[1 + nonce.length + ciphertext.length];
            encrypted[0] = FORMAT_VERSION;
            System.arraycopy(nonce, 0, encrypted, 1, nonce.length);
            System.arraycopy(ciphertext, 0, encrypted, 1 + nonce.length, ciphertext.length);
            return encrypted;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not encrypt the face embedding", e);
        }
    }

    public double[] decrypt(byte[] encryptedEmbedding) {
        if (encryptedEmbedding == null || encryptedEmbedding.length <= 1 + NONCE_LENGTH_BYTES
                || encryptedEmbedding[0] != FORMAT_VERSION) {
            throw new IllegalArgumentException("Stored face embedding has an unsupported format");
        }
        byte[] nonce = Arrays.copyOfRange(encryptedEmbedding, 1, 1 + NONCE_LENGTH_BYTES);
        byte[] ciphertext = Arrays.copyOfRange(encryptedEmbedding, 1 + NONCE_LENGTH_BYTES,
                encryptedEmbedding.length);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            ByteBuffer plaintext = ByteBuffer.wrap(cipher.doFinal(ciphertext));
            if (plaintext.remaining() < Integer.BYTES) {
                throw new IllegalArgumentException("Stored face embedding is incomplete");
            }
            int length = plaintext.getInt();
            validateLength(length);
            if (plaintext.remaining() != length * Double.BYTES) {
                throw new IllegalArgumentException("Stored face embedding has an invalid length");
            }
            double[] embedding = new double[length];
            for (int i = 0; i < length; i++) {
                embedding[i] = plaintext.getDouble();
                if (!Double.isFinite(embedding[i]) || Math.abs(embedding[i]) > 10) {
                    throw new IllegalArgumentException("Stored face embedding contains an invalid value");
                }
            }
            return embedding;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not decrypt the face embedding", e);
        }
    }

    private void validateLength(int length) {
        if (length < MIN_EMBEDDING_LENGTH || length > MAX_EMBEDDING_LENGTH) {
            throw new IllegalArgumentException("A face embedding with 64 to 4096 values is required");
        }
    }

    private SecretKeySpec key() {
        try {
            byte[] raw = Base64.getDecoder().decode(encodedKey == null ? "" : encodedKey.trim());
            if (raw.length != KEY_LENGTH_BYTES) {
                throw new IllegalArgumentException("Expected a 32-byte AES key");
            }
            return new SecretKeySpec(raw, "AES");
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Face encryption is not configured; set FACE_ENCRYPTION_KEY to a Base64-encoded 32-byte key");
        }
    }
}
