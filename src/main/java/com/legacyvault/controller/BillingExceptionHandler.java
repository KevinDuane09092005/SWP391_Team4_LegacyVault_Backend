package com.legacyvault.controller;

import com.legacyvault.dto.BillingErrorResponse;
import com.legacyvault.service.BillingException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = {BillingController.class, NotificationController.class})
public class BillingExceptionHandler {

    @ExceptionHandler(BillingException.class)
    public ResponseEntity<BillingErrorResponse> handleBilling(BillingException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(new BillingErrorResponse(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<BillingErrorResponse> handleValidation(Exception exception) {
        return ResponseEntity.badRequest()
                .body(new BillingErrorResponse("VALIDATION_ERROR", "The request is invalid."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BillingErrorResponse> handleUnexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new BillingErrorResponse("INTERNAL_ERROR", "An unexpected billing error occurred."));
    }
}
