package com.legacyvault.model.dto;

import java.util.Map;

public record ErrorResponse(ErrorBody error) {
    public record ErrorBody(String code, String message, Map<String, Object> details) {
    }
}
