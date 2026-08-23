package com.fininsight.common.exception;

import com.fininsight.common.dto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized exception handler for all REST controllers.
 * Converts exceptions to consistent ErrorResponse structures.
 * Never exposes internal stack traces to API consumers.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.of(
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value(),
                getPath(request));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, WebRequest request) {
        log.warn("Business rule violation: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.of(
                ex.getMessage(),
                ex.getStatus().value(),
                getPath(request));
        return ResponseEntity.status(ex.getStatus()).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("Validation failed: {}", fieldErrors);
        ErrorResponse error = ErrorResponse.of(
                "Validation failed",
                HttpStatus.BAD_REQUEST.value(),
                getPath(request),
                fieldErrors);
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, WebRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String fieldName = violation.getPropertyPath().toString();
            fieldErrors.put(fieldName, violation.getMessage());
        });
        log.warn("Constraint violation: {}", fieldErrors);
        ErrorResponse error = ErrorResponse.of(
                "Validation failed",
                HttpStatus.BAD_REQUEST.value(),
                getPath(request),
                fieldErrors);
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, WebRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.of(
                "Access denied",
                HttpStatus.FORBIDDEN.value(),
                getPath(request));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException ex, WebRequest request) {
        log.warn("Authentication failed: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.of(
                "Authentication failed",
                HttpStatus.UNAUTHORIZED.value(),
                getPath(request));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            org.springframework.dao.DataIntegrityViolationException ex, WebRequest request) {
        log.warn("Database constraint or data integrity violation: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.of(
                "A resource conflict or constraint violation occurred",
                HttpStatus.CONFLICT.value(),
                getPath(request));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex, WebRequest request) {
        String paramName = ex.getName();
        String message = String.format("Invalid parameter: '%s' has an invalid format or value", paramName);
        log.warn("Method argument type mismatch for param {}: {}", paramName, ex.getMessage());
        ErrorResponse error = ErrorResponse.of(
                message,
                HttpStatus.BAD_REQUEST.value(),
                getPath(request));
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            org.springframework.http.converter.HttpMessageNotReadableException ex, WebRequest request) {
        log.warn("Malformed JSON request body: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.of(
                "Malformed JSON request body or invalid format",
                HttpStatus.BAD_REQUEST.value(),
                getPath(request));
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            org.springframework.web.HttpRequestMethodNotSupportedException ex, WebRequest request) {
        log.warn("HTTP method not supported: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.of(
                String.format("HTTP method '%s' is not supported for this endpoint", ex.getMethod()),
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                getPath(request));
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {
        log.error("Unexpected error occurred", ex);
        ErrorResponse error = ErrorResponse.of(
                "An unexpected error occurred. Please try again later.",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                getPath(request));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private String getPath(WebRequest request) {
        if (request instanceof ServletWebRequest servletRequest) {
            return servletRequest.getRequest().getRequestURI();
        }
        return "unknown";
    }
}
