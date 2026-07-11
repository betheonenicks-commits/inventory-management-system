package com.iams.common.web;

import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.OptimisticLockConflictException;
import com.iams.common.exception.ValidationErrorItem;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.InvalidCredentialsException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.MDC;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Central mapper from Java exceptions to RFC 7807 problem+json, per API spec Section 1.9.
 * Every branch returns the identical envelope shape: type/title/status/detail/instance/
 * errorCode/traceId/timestamp, with `errors[]` added for field-level validation failures.
 *
 * Note: this handler cannot cover 401/403 - Spring Security's filter chain runs in front
 * of DispatcherServlet, so those are handled by JsonAuthenticationEntryPoint /
 * JsonAccessDeniedHandler in common.security, using the identical shape below.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final String PROBLEM_BASE = "https://iams.internal/problems/";

    @ExceptionHandler(ValidationFailedException.class)
    public ResponseEntity<ProblemDetail> handleValidationFailed(ValidationFailedException ex, HttpServletRequest req) {
        ProblemDetail pd = build(HttpStatus.BAD_REQUEST, "validation-failed", "Validation Failed",
                "One or more fields failed validation.", "VALIDATION_FAILED", req);
        pd.setProperty("errors", ex.getErrors());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleBeanValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ValidationErrorItem> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ValidationErrorItem(fe.getField(), fe.getDefaultMessage()))
                .collect(Collectors.toList());
        ProblemDetail pd = build(HttpStatus.BAD_REQUEST, "validation-failed", "Validation Failed",
                "One or more fields failed validation.", "VALIDATION_FAILED", req);
        pd.setProperty("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        ProblemDetail pd = build(HttpStatus.BAD_REQUEST, "malformed-request", "Malformed Request",
                "The request body could not be parsed.", "VALIDATION_FAILED", req);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @ExceptionHandler(OptimisticLockConflictException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLockConflict(OptimisticLockConflictException ex, HttpServletRequest req) {
        ProblemDetail pd = build(HttpStatus.CONFLICT, "optimistic-lock-conflict", "Optimistic Lock Conflict",
                ex.getMessage(), "OPTIMISTIC_LOCK_CONFLICT", req);
        pd.setProperty("expectedVersion", ex.getExpectedVersion());
        pd.setProperty("currentVersion", ex.getCurrentVersion());
        pd.setProperty("currentResource", ex.getCurrentResource());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleHibernateOptimisticLock(OptimisticLockingFailureException ex, HttpServletRequest req) {
        ProblemDetail pd = build(HttpStatus.CONFLICT, "optimistic-lock-conflict", "Optimistic Lock Conflict",
                "The resource was modified by another request. Reload and retry.", "OPTIMISTIC_LOCK_CONFLICT", req);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ProblemDetail> handleConflict(ConflictException ex, HttpServletRequest req) {
        ProblemDetail pd = build(HttpStatus.CONFLICT, "conflict", "Conflict", ex.getMessage(), ex.getErrorCode(), req);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest req) {
        ProblemDetail pd = build(HttpStatus.UNAUTHORIZED, "invalid-credentials", "Invalid Credentials",
                ex.getMessage(), "AUTH_INVALID_CREDENTIALS", req);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(NotFoundException ex, HttpServletRequest req) {
        ProblemDetail pd = build(HttpStatus.NOT_FOUND, "not-found", "Not Found", ex.getMessage(), "NOT_FOUND", req);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        ProblemDetail pd = build(HttpStatus.METHOD_NOT_ALLOWED, "method-not-allowed", "Method Not Allowed",
                "This resource does not support direct mutation. Use the correct state-transition endpoint.",
                "METHOD_NOT_ALLOWED", req);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(pd);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(Exception ex, HttpServletRequest req) {
        ProblemDetail pd = build(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", "Internal Server Error",
                "An unexpected error occurred.", "INTERNAL_ERROR", req);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd);
    }

    private ProblemDetail build(HttpStatus status, String slug, String title, String detail, String errorCode, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(PROBLEM_BASE + slug));
        pd.setTitle(title);
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("errorCode", errorCode);
        pd.setProperty("traceId", MDC.get(CorrelationIdFilter.MDC_KEY));
        pd.setProperty("timestamp", Instant.now().toString());
        return pd;
    }
}
