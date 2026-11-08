package com.legacyvault.controller;

import com.legacyvault.exception.AdminApiException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = {
        AdminAuditController.class,
        AdminUserController.class,
        AdminSettingsController.class
})
public class AdminApiExceptionHandler {

    @ExceptionHandler(AdminApiException.class)
    public ResponseEntity<Map<String, String>> handleAdminApiException(AdminApiException exception) {
        String message = exception.getMessage() != null ? exception.getMessage() : "Request failed.";
        return ResponseEntity.status(exception.getStatus())
                .body(Map.of(
                        "code", exception.getCode(),
                        "message", message,
                        "error", message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException exception) {
        String message = exception.getMessage() != null ? exception.getMessage() : "Invalid input.";
        return ResponseEntity.badRequest()
                .body(Map.of(
                        "code", "INVALID_INPUT",
                        "message", message,
                        "error", message));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<Map<String, String>> handleValidationException(Exception exception) {
        String message = "Some values are not valid.";
        return ResponseEntity.badRequest()
                .body(Map.of(
                        "code", "INVALID_INPUT",
                        "message", message,
                        "error", message));
    }
}

