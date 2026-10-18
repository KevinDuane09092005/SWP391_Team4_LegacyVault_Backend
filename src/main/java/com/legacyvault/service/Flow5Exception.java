package com.legacyvault.service;

import org.springframework.http.HttpStatus;

public class Flow5Exception extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public Flow5Exception(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }

    public static Flow5Exception unauthenticated() {
        return new Flow5Exception(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Sign-in is required.");
    }

    public static Flow5Exception forbidden() {
        return new Flow5Exception(HttpStatus.FORBIDDEN, "FORBIDDEN", "You are not allowed to perform this action.");
    }

    public static Flow5Exception notFound() {
        return new Flow5Exception(HttpStatus.NOT_FOUND, "NOT_FOUND", "The requested case or document was not found.");
    }

    public static Flow5Exception conflict(String message) {
        return new Flow5Exception(HttpStatus.CONFLICT, "CONFLICT", message);
    }

    public static Flow5Exception unavailable(String message) {
        return new Flow5Exception(HttpStatus.SERVICE_UNAVAILABLE, "INTEGRATION_NOT_CONFIGURED", message);
    }

    public static Flow5Exception invalid(String code, String message) {
        return new Flow5Exception(HttpStatus.UNPROCESSABLE_ENTITY, code, message);
    }
}
