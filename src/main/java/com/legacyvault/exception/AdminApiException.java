package com.legacyvault.exception;

import org.springframework.http.HttpStatus;

public class AdminApiException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public AdminApiException(HttpStatus status, String message) {
        this(status, defaultCode(status), message);
    }

    public AdminApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code != null && !code.isBlank() ? code : defaultCode(status);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public static AdminApiException badRequest(String message) {
        return new AdminApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", message);
    }

    public static AdminApiException badRequest(String code, String message) {
        return new AdminApiException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static AdminApiException notFound(String message) {
        return new AdminApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    public static AdminApiException notFound(String code, String message) {
        return new AdminApiException(HttpStatus.NOT_FOUND, code, message);
    }

    public static AdminApiException conflict(String message) {
        return new AdminApiException(HttpStatus.CONFLICT, "CONFLICT", message);
    }

    public static AdminApiException conflict(String code, String message) {
        return new AdminApiException(HttpStatus.CONFLICT, code, message);
    }

    private static String defaultCode(HttpStatus status) {
        if (status == HttpStatus.NOT_FOUND) return "NOT_FOUND";
        if (status == HttpStatus.CONFLICT) return "CONFLICT";
        if (status == HttpStatus.UNAUTHORIZED) return "NOT_SIGNED_IN";
        if (status == HttpStatus.FORBIDDEN) return "ADMIN_ONLY";
        return "INVALID_INPUT";
    }
}

