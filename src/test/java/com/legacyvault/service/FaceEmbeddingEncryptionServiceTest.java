package com.legacyvault.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class FaceEmbeddingEncryptionServiceTest {

    private static final String TEST_KEY = Base64.getEncoder().encodeToString(new byte[32]);

    @Test
    void encryptsAndDecryptsEmbedding() {
        FaceEmbeddingEncryptionService service = new FaceEmbeddingEncryptionService(TEST_KEY);
        double[] embedding = new double[128];
        Arrays.fill(embedding, 0.25);

        byte[] encrypted = service.encrypt(embedding);

        assertFalse(Arrays.equals(toBytes(embedding), encrypted));
        assertArrayEquals(embedding, service.decrypt(encrypted));
    }

    @Test
    void rejectsTamperedCiphertext() {
        FaceEmbeddingEncryptionService service = new FaceEmbeddingEncryptionService(TEST_KEY);
        byte[] encrypted = service.encrypt(new double[128]);
        encrypted[encrypted.length - 1] ^= 1;

        assertThrows(IllegalStateException.class, () -> service.decrypt(encrypted));
    }

    @Test
    void requiresConfiguredThirtyTwoByteKey() {
        FaceEmbeddingEncryptionService service = new FaceEmbeddingEncryptionService("");

        assertThrows(ResponseStatusException.class, () -> service.encrypt(new double[128]));
    }

    private byte[] toBytes(double[] values) {
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(values.length * Double.BYTES);
        for (double value : values) buffer.putDouble(value);
        return buffer.array();
    }
}
