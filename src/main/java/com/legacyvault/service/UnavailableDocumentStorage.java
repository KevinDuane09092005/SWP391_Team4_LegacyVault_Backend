package com.legacyvault.service;

import org.springframework.stereotype.Component;

@Component
public class UnavailableDocumentStorage implements DocumentStorage {
    @Override
    public StoredDocument store(String documentId, byte[] content) {
        throw Flow5Exception.unavailable("A private encrypted document-storage adapter has not been connected yet.");
    }

    @Override
    public byte[] read(String storageKey) {
        throw Flow5Exception.unavailable("A private encrypted document-storage adapter has not been connected yet.");
    }

    @Override
    public void delete(String storageKey) {
        throw Flow5Exception.unavailable("A private encrypted document-storage adapter has not been connected yet.");
    }
}
