package com.legacyvault.service;

public record StoredContent(String fileName, String mediaType, byte[] bytes) {
}
