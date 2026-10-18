package com.legacyvault.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalEncryptedDocumentStorageTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void storesEncryptedBytesAndCanReadThemBack() throws Exception {
        LocalEncryptedDocumentStorage storage = new LocalEncryptedDocumentStorage(
                temporaryDirectory.toString(), "test-only-key-that-is-at-least-32-characters-long");
        byte[] original = "private legal evidence".getBytes(StandardCharsets.UTF_8);

        DocumentStorage.StoredDocument stored = storage.store(UUID.randomUUID().toString(), original);
        Path encryptedFile = temporaryDirectory.resolve(stored.storageKey());

        assertTrue(Files.exists(encryptedFile));
        assertFalse(java.util.Arrays.equals(original, Files.readAllBytes(encryptedFile)));
        assertArrayEquals(original, storage.read(stored.storageKey()));
        assertTrue(stored.sha256().matches("[0-9a-f]{64}"));

        storage.delete(stored.storageKey());
        assertFalse(Files.exists(encryptedFile));
    }

    @Test
    void refusesToStoreWhenNoEncryptionKeyIsConfigured() {
        LocalEncryptedDocumentStorage storage = new LocalEncryptedDocumentStorage(
                temporaryDirectory.toString(), "");

        Flow5Exception error = assertThrows(Flow5Exception.class,
                () -> storage.store(UUID.randomUUID().toString(), new byte[]{1, 2, 3}));

        assertTrue(error.getMessage().contains("FLOW5_DOCUMENT_STORAGE_ENCRYPTION_KEY"));
    }

    @Test
    void rejectsStorageKeysThatCouldEscapeThePrivateDirectory() {
        LocalEncryptedDocumentStorage storage = new LocalEncryptedDocumentStorage(
                temporaryDirectory.toString(), "test-only-key-that-is-at-least-32-characters-long");

        assertThrows(Flow5Exception.class, () -> storage.read("..\\secret.enc"));
    }
}
