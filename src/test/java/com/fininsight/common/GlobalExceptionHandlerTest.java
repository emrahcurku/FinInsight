package com.fininsight.common;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.fininsight.common.dto.ErrorResponse;
import com.fininsight.common.exception.BusinessException;
import com.fininsight.common.exception.GlobalExceptionHandler;
import com.fininsight.common.exception.ResourceNotFoundException;

public class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();
    private WebRequest webRequest;

    @BeforeEach
    public void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/test");
        webRequest = new ServletWebRequest(request);
    }

    @Test
    @DisplayName("ResourceNotFoundException maps to 404 NOT_FOUND")
    void testHandleResourceNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Category not found");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleResourceNotFound(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().message()).isEqualTo("Category not found");
        assertThat(response.getBody().path()).isEqualTo("/api/v1/test");
    }

    @Test
    @DisplayName("BusinessException maps to specified HTTP status code")
    void testHandleBusinessException() {
        BusinessException ex = new BusinessException("Cannot delete system category", HttpStatus.FORBIDDEN);
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBusinessException(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(403);
        assertThat(response.getBody().message()).isEqualTo("Cannot delete system category");
    }

    @Test
    @DisplayName("AccessDeniedException maps to 403 FORBIDDEN")
    void testHandleAccessDenied() {
        AccessDeniedException ex = new AccessDeniedException("Access is denied");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAccessDenied(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(403);
        assertThat(response.getBody().message()).isEqualTo("Access denied");
    }

    @Test
    @DisplayName("AuthenticationException maps to 401 UNAUTHORIZED")
    void testHandleAuthentication() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAuthentication(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(401);
        assertThat(response.getBody().message()).isEqualTo("Authentication failed");
    }

    @Test
    @DisplayName("DataIntegrityViolationException maps to 409 CONFLICT")
    void testHandleDataIntegrityViolation() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Unique index violation");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleDataIntegrityViolation(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().message()).contains("conflict or constraint violation");
    }

    @Test
    @DisplayName("MethodArgumentTypeMismatchException maps to 400 BAD_REQUEST")
    void testHandleTypeMismatch() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "invalid-uuid", UUIDType.class, "id", null, null);
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleTypeMismatch(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().message()).contains("Invalid parameter: 'id'");
    }

    @Test
    @DisplayName("HttpMessageNotReadableException maps to 400 BAD_REQUEST")
    void testHandleHttpMessageNotReadable() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON parse error", new MockHttpInputMessage(new byte[0]));
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleHttpMessageNotReadable(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().message()).contains("Malformed JSON request body");
    }

    @Test
    @DisplayName("HttpRequestMethodNotSupportedException maps to 405 METHOD_NOT_ALLOWED")
    void testHandleMethodNotSupported() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("POST");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMethodNotSupported(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(405);
        assertThat(response.getBody().message()).contains("HTTP method 'POST' is not supported");
    }

    @Test
    @DisplayName("Generic unhandled exception maps to safe 500 without leaking internal exception details")
    void testHandleGenericException() {
        RuntimeException ex = new RuntimeException("Sensitive database stacktrace or internal table details");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex, webRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred. Please try again later.");
        assertThat(response.getBody().message()).doesNotContain("Sensitive database");
    }

    @Test
    @DisplayName("ErrorResponse includes correlationId from MDC context")
    void testErrorResponseIncludesCorrelationId() {
        org.slf4j.MDC.put("correlationId", "test-corr-id-999");
        try {
            ResourceNotFoundException ex = new ResourceNotFoundException("Not found");
            ResponseEntity<ErrorResponse> response = exceptionHandler.handleResourceNotFound(ex, webRequest);

            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().correlationId()).isEqualTo("test-corr-id-999");
        } finally {
            org.slf4j.MDC.clear();
        }
    }

    private static class UUIDType {}
}
