package com.legacyvault.model.dto;

import com.legacyvault.model.entity.DocumentType;

import java.time.Instant;

public record CaseDocumentResponse(
        String documentId,
        DocumentType documentType,
        String fileName,
        String mediaType,
        long sizeBytes,
        String uploadedByUserId,
        Instant uploadedAt
) {
}
