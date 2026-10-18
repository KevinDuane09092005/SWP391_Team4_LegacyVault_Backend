package com.legacyvault.service;

public interface DocumentStorage {
    StoredDocument store(String documentId, byte[] content);
    byte[] read(String storageKey);
    void delete(String storageKey);

    record StoredDocument(String storageKey, String sha256) {
    }
}
