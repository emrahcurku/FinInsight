package com.fininsight.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a business rule is violated.
 * Handled by GlobalExceptionHandler → HTTP status determined by the exception.
 *
 * Examples: duplicate email, budget already exists for this month, etc.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
    }

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
