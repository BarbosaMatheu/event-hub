package com.eventhub.event_hub.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * Maps every exception to the single {@link StandardError} format. Order of thought:
 * domain exceptions first, then security, then request-parsing problems, and finally a
 * catch-all that never leaks internals.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ------------------------------------------------------------------ domain

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<StandardError> notFound(ResourceNotFoundException e, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Resource Not Found", e.getMessage(), request);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<StandardError> business(BusinessException e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Business Rule Violation", e.getMessage(), request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<StandardError> conflict(ConflictException e, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Conflict", e.getMessage(), request);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<StandardError> tooManyRequests(TooManyRequestsException e, HttpServletRequest request) {
        ResponseEntity<StandardError> response = build(HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", e.getMessage(), request);
        return ResponseEntity.status(response.getStatusCode())
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(e.getRetryAfterSeconds()))
                .body(response.getBody());
    }

    // ------------------------------------------------------------------ security

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<StandardError> authentication(AuthenticationException e, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid login or password", request);
    }

    /** Thrown by @PreAuthorize (method security), which happens inside MVC, not in the filter chain. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<StandardError> accessDenied(AccessDeniedException e, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, "Forbidden", "You do not have permission to perform this action", request);
    }

    // ------------------------------------------------------------------ request problems

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardError> validation(MethodArgumentNotValidException e, HttpServletRequest request) {
        List<FieldViolation> violations = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldViolation(fe.getField(), fe.getDefaultMessage()))
                .toList();
        String message = violations.isEmpty() ? "Invalid request" : "Validation failed for " + violations.size() + " field(s)";
        StandardError body = new StandardError(java.time.Instant.now(), 400, "Validation Error", message,
                request.getRequestURI(), violations);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<StandardError> unreadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Malformed Request", "The request body is missing or is not valid JSON", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<StandardError> typeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Bad Request", "Invalid value for parameter '" + e.getName() + "'", request);
    }

    // ------------------------------------------------------------------ persistence

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<StandardError> optimisticLock(ObjectOptimisticLockingFailureException e, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Conflict", "The resource was modified by someone else. Reload it and try again.", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<StandardError> integrity(DataIntegrityViolationException e, HttpServletRequest request) {
        log.warn("Data integrity violation on {}: {}", request.getRequestURI(), e.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "Conflict", "The operation conflicts with existing data", request);
    }

    // ------------------------------------------------------------------ catch-all

    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardError> unexpected(Exception e, HttpServletRequest request) {
        // Spring MVC's own exceptions (405, 415, 404 for unknown routes, missing parameters...)
        // implement ErrorResponse and already carry the right status. Reuse it.
        if (e instanceof ErrorResponse errorResponse) {
            HttpStatusCode status = errorResponse.getStatusCode();
            String reason = HttpStatus.resolve(status.value()) != null
                    ? HttpStatus.valueOf(status.value()).getReasonPhrase()
                    : "Error";
            String detail = errorResponse.getBody().getDetail();
            return ResponseEntity.status(status)
                    .body(StandardError.of(status.value(), reason, detail != null ? detail : reason, request.getRequestURI()));
        }
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred", request);
    }

    private ResponseEntity<StandardError> build(HttpStatus status, String error, String message, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(StandardError.of(status.value(), error, message, request.getRequestURI()));
    }
}
