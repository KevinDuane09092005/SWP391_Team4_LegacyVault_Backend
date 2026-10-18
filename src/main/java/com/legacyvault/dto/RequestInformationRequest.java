package com.legacyvault.dto;

import com.legacyvault.entity.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RequestInformationRequest(
        @NotBlank @Size(max = 2000) String reason,
        @Size(max = 6) List<DocumentType> requestedDocumentTypes
) {
}
