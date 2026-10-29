package com.legacyvault.exception;

import org.springframework.http.HttpStatus;

public class AdminApiException extends RuntimeException {
    private final HttpStatus status;

    public AdminApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static AdminApiException badRequest(String message) {
        return new AdminApiException(HttpStatus.BAD_REQUEST, message);
    }

    public static AdminApiException notFound(String message) {
        return new AdminApiException(HttpStatus.NOT_FOUND, message);
    }

    public static AdminApiException conflict(String message) {
        return new AdminApiException(HttpStatus.CONFLICT, message);
    }
}
