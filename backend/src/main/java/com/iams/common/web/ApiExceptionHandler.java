package com.iams.common.web;

import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.OptimisticLockConflictException;
import com.iams.common.exception.ValidationErrorItem;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.AccountLockedException;
import com.iams.common.security.InvalidCredentialsException;
import com.iams.common.security.InvalidRefreshTokenException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
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

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
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
        // type slug is derived from errorCode (e.g. USER_HAS_OUTSTANDING_ASSIGNMENTS ->
        // user-has-outstanding-assignments) rather than a single generic "conflict" slug,
        // matching the per-scenario `type` URIs the API spec documents (Section 4.5).
        String typeSlug = ex.getErrorCode().toLowerCase().replace('_', '-');
        ProblemDetail pd = build(HttpStatus.CONFLICT, typeSlug, ex.getTitle(), ex.getMessage(), ex.getErrorCode(), req);
        ex.getExtraProperties().forEach(pd::setProperty);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest req) {
        ProblemDetail pd = build(HttpStatus.UNAUTHORIZED, "invalid-credentials", "Invalid Credentials",
                ex.getMessage(), "AUTH_INVALID_CREDENTIALS", req);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ProblemDetail> handleInvalidRefreshToken(InvalidRefreshTokenException ex, HttpServletRequest req) {
        ProblemDetail pd = build(HttpStatus.UNAUTHORIZED, "invalid-refresh-token", "Invalid Refresh Token",
                ex.getMessage(), "AUTH_INVALID_REFRESH_TOKEN", req);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ProblemDetail> handleAccountLocked(AccountLockedException ex, HttpServletRequest req) {
        ProblemDetail pd = build(HttpStatus.LOCKED, "account-locked", "Account Locked", ex.getMessage(), "ACCOUNT_LOCKED", req);
        pd.setProperty("lockedUntil", ex.getLockedUntil());
        return ResponseEntity.status(HttpStatus.LOCKED).body(pd);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        // Covers both org.springframework.security.authorization.AuthorizationDeniedException
        // (@PreAuthorize denials) and this plain AccessDeniedException thrown directly by
        // application code (OrgScopeGuard.requireWithinScope) - the former extends the
        // latter, so one handler catches both. Either way, the denial is thrown from
        // inside the handler method invocation (AOP around @PreAuthorize, or a plain
        // method call for OrgScopeGuard), so DispatcherServlet's own exception resolution
        // (this @RestControllerAdvice) catches it before it ever reaches the filter chain -
        // JsonAccessDeniedHandler (which handles filter-chain-level denials, per its own
        // comment) never sees either case. Without this handler they fell through to
        // handleGeneric() below and returned a 500, not a 403 - found via live
        // click-testing for the OrgScopeGuard case specifically (the @PreAuthorize case
        // was caught and fixed earlier the same day).
        ProblemDetail pd = build(HttpStatus.FORBIDDEN, "forbidden", "Forbidden",
                "You do not have permission to perform this action.", "FORBIDDEN", req);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
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
        log.error("Unhandled exception for {} {} (traceId={})", req.getMethod(), req.getRequestURI(),
                pd.getProperties().get("traceId"), ex);
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
