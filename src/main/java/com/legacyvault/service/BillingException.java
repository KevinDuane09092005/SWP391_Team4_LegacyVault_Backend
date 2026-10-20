package com.legacyvault.service;

import org.springframework.http.HttpStatus;

public class BillingException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public BillingException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }

    public static BillingException badRequest(String code, String message) {
        return new BillingException(HttpStatus.BAD_REQUEST, code, message);
    }

    public static BillingException unauthorized() {
        return new BillingException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Sign in to continue.");
    }

    public static BillingException forbidden() {
        return new BillingException(HttpStatus.FORBIDDEN, "FORBIDDEN", "This action is not allowed for this account.");
    }

    public static BillingException notFound() {
        return new BillingException(HttpStatus.NOT_FOUND, "NOT_FOUND", "The requested billing record was not found.");
    }

    public static BillingException conflict(String message) {
        return new BillingException(HttpStatus.CONFLICT, "CONFLICT", message);
    }
}
