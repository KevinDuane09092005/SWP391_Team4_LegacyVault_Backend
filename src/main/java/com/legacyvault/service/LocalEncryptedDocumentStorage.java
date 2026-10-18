package com.legacyvault.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.UUID;

@Component
public class LocalEncryptedDocumentStorage implements DocumentStorage {

    private static final byte[] FILE_MAGIC = {'L', 'V', 'F', '5'};
    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;

    private final Path root;
    private final String configuredKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public LocalEncryptedDocumentStorage(
            @Value("${flow5.document-storage.path:./var/private/legal-verification}") String directory,
            @Value("${flow5.document-storage.encryption-key:}") String configuredKey) {
        this.root = Path.of(directory).toAbsolutePath().normalize();
        this.configuredKey = configuredKey;
    }

    @Override
    public StoredDocument store(String documentId, byte[] content) {
        SecretKeySpec key = encryptionKey();
        UUID id;
        try {
            id = UUID.fromString(documentId);
        } catch (IllegalArgumentException exception) {
            throw Flow5Exception.invalid("INVALID_DOCUMENT_ID", "The document identifier is invalid.");
        }

        byte[] iv = new byte[IV_LENGTH];
        secureRandom.nextBytes(iv);
        byte[] encrypted = crypt(Cipher.ENCRYPT_MODE, key, iv, content);
        String storageKey = id + ".enc";
        Path destination = resolve(storageKey);
        try {
            Files.createDirectories(root);
            ByteBuffer payload = ByteBuffer.allocate(FILE_MAGIC.length + iv.length + encrypted.length);
            payload.put(FILE_MAGIC).put(iv).put(encrypted);
            Files.write(destination, payload.array(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return new StoredDocument(storageKey, sha256(content));
        } catch (IOException exception) {
            throw Flow5Exception.unavailable("Private document storage is not available.");
        }
    }

    @Override
    public byte[] read(String storageKey) {
        SecretKeySpec key = encryptionKey();
        Path source = resolve(storageKey);
        try {
            byte[] payload = Files.readAllBytes(source);
            if (payload.length <= FILE_MAGIC.length + IV_LENGTH
                    || !startsWith(payload, FILE_MAGIC)) {
                throw Flow5Exception.unavailable("The stored document could not be read.");
            }
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(payload, FILE_MAGIC.length, iv, 0, IV_LENGTH);
            byte[] encrypted = new byte[payload.length - FILE_MAGIC.length - IV_LENGTH];
            System.arraycopy(payload, FILE_MAGIC.length + IV_LENGTH, encrypted, 0, encrypted.length);
            return crypt(Cipher.DECRYPT_MODE, key, iv, encrypted);
        } catch (IOException exception) {
            throw Flow5Exception.notFound();
        }
    }

    @Override
    public void delete(String storageKey) {
        Path file = resolve(storageKey);
        try {
            Files.deleteIfExists(file);
        } catch (IOException exception) {
            throw Flow5Exception.unavailable("Private document storage cleanup failed.");
        }
    }

    private SecretKeySpec encryptionKey() {
        if (configuredKey == null || configuredKey.length() < 32) {
            throw Flow5Exception.unavailable(
                    "Set FLOW5_DOCUMENT_STORAGE_ENCRYPTION_KEY to a secret of at least 32 characters before uploading documents.");
        }
        try {
            byte[] key = MessageDigest.getInstance("SHA-256")
                    .digest(configuredKey.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(key, "AES");
        } catch (Exception exception) {
            throw new IllegalStateException("AES-GCM is unavailable.", exception);
        }
    }

    private byte[] crypt(int mode, SecretKeySpec key, byte[] iv, byte[] input) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(mode, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            cipher.updateAAD(FILE_MAGIC);
            return cipher.doFinal(input);
        } catch (Exception exception) {
            throw Flow5Exception.unavailable("The stored document could not be encrypted or authenticated.");
        }
    }

    private Path resolve(String storageKey) {
        if (storageKey == null || !storageKey.matches(
                "(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.enc$")) {
            throw Flow5Exception.notFound();
        }
        Path resolved = root.resolve(storageKey).normalize();
        if (!resolved.startsWith(root)) {
            throw Flow5Exception.notFound();
        }
        return resolved;
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) return false;
        for (int index = 0; index < prefix.length; index++) {
            if (value[index] != prefix[index]) return false;
        }
        return true;
    }
}
