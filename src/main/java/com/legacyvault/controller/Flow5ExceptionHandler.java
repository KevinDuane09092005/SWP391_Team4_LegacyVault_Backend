package com.legacyvault.controller;

import com.legacyvault.dto.ErrorResponse;
import com.legacyvault.service.Flow5Exception;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.Map;

@RestControllerAdvice(assignableTypes = LegalVerificationCaseController.class)
public class Flow5ExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(Flow5ExceptionHandler.class);

    @ExceptionHandler(Flow5Exception.class)
    public ResponseEntity<ErrorResponse> handleFlow5(Flow5Exception exception) {
        return response(exception.getStatus(), exception.getCode(), exception.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, Object> details = Map.of("fields", exception.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(), "message", error.getDefaultMessage()))
                .toList());
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "The request is invalid.", details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "The request is invalid.", null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException exception) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_BODY", "The request body is missing or malformed.", null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", "A query or form parameter is invalid.", null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleLargeUpload(MaxUploadSizeExceededException exception) {
        return response(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "Each document must be 10 MB or smaller.", null);
    }

    @ExceptionHandler({DataIntegrityViolationException.class, ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<ErrorResponse> handleConflict(RuntimeException exception) {
        return response(HttpStatus.CONFLICT, "CONFLICT", "The record changed or conflicts with an existing case.", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        log.error("Unexpected Flow 5 API error", exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected server error occurred.", null);
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, String code, String message,
                                                   Map<String, Object> details) {
        return ResponseEntity.status(status).body(new ErrorResponse(
                new ErrorResponse.ErrorBody(code, message, details)));
    }
}
