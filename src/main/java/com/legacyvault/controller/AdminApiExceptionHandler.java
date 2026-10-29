package com.legacyvault.controller;

import com.legacyvault.exception.AdminApiException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = {AdminAuditController.class, AdminUserController.class})
public class AdminApiExceptionHandler {

    @ExceptionHandler(AdminApiException.class)
    public ResponseEntity<Map<String, String>> handleAdminApiException(AdminApiException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(Map.of("message", exception.getMessage(), "error", exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", exception.getMessage(), "error", exception.getMessage()));
    }
}
