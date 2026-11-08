package com.legacyvault.chatbot.controller;

import com.legacyvault.chatbot.dto.ChatbotErrorResponse;
import com.legacyvault.chatbot.service.ChatbotException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ChatbotController.class)
public class ChatbotExceptionHandler {

    @ExceptionHandler(ChatbotException.class)
    public ResponseEntity<ChatbotErrorResponse> handleChatbot(ChatbotException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(new ChatbotErrorResponse(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class,
            HttpMessageNotReadableException.class})
    public ResponseEntity<ChatbotErrorResponse> handleValidation(Exception exception) {
        return ResponseEntity.badRequest().body(
                new ChatbotErrorResponse("VALIDATION_ERROR", "The chatbot request is invalid."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ChatbotErrorResponse> handleUnexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ChatbotErrorResponse("INTERNAL_ERROR", "The chatbot could not complete the request."));
    }
}
