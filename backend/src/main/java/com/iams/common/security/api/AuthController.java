package com.iams.common.security.api;

import com.iams.common.exception.NotFoundException;
import com.iams.common.security.AccountLockedException;
import com.iams.common.security.CurrentUser;
import com.iams.common.security.CurrentUserProvider;
import com.iams.common.security.InvalidCredentialsException;
import com.iams.common.security.InvalidRefreshTokenException;
import com.iams.common.security.JwtService;
import com.iams.common.security.SessionActivityGuard;
import com.iams.common.security.StepUpGuard;
import com.iams.sec.application.SecurityEventLogger;
import com.iams.sec.domain.SecurityEventType;
import com.iams.usr.application.RefreshTokenService;
import com.iams.usr.application.UserLockoutService;
import com.iams.usr.application.UserQueryService;
import com.iams.usr.application.UserWithRoles;
import com.iams.usr.domain.AppUser;
import com.iams.usr.domain.Role;
import com.iams.usr.domain.UserStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Real, DB-backed authentication (FR-SEC-01, US-USR-01), replacing the
 * DevSecurityProperties.DevUser hardcoded single-user stub. The token
 * contract (CurrentUser, JwtService) is unchanged - only where the
 * credentials/roles come from has changed.
 * <p>
 * Deliberately unimplemented here (left for their own SEC stories, not
 * silently assumed): MFA (US-SEC-03a/US-SEC-17).
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserQueryService userQueryService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CurrentUserProvider currentUserProvider;
    private final UserLockoutService lockoutService;
    private final SecurityEventLogger securityEventLogger;
    private final RefreshTokenService refreshTokenService;
    private final SessionActivityGuard sessionActivityGuard;
    private final StepUpGuard stepUpGuard;
    // A real BCrypt hash of a value nobody's actual password will ever be, encoded once at
    // startup and compared against on every login for a username that doesn't exist. This
    // keeps response timing (dominated by BCrypt's cost) the same whether the username is
    // real or not - the AC-SEC-04-X guarantee this class already claims ("the API never
    // leaks whether a given username exists") previously only held for the response body,
    // not for wall-clock timing, since an unknown username used to skip the BCrypt check
    // entirely and fail immediately.
    private final String dummyPasswordHash;

    public AuthController(UserQueryService userQueryService, PasswordEncoder passwordEncoder,
                           JwtService jwtService, CurrentUserProvider currentUserProvider,
                           UserLockoutService lockoutService, SecurityEventLogger securityEventLogger,
                           RefreshTokenService refreshTokenService, SessionActivityGuard sessionActivityGuard,
                           StepUpGuard stepUpGuard) {
        this.userQueryService = userQueryService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.currentUserProvider = currentUserProvider;
        this.lockoutService = lockoutService;
        this.securityEventLogger = securityEventLogger;
        this.refreshTokenService = refreshTokenService;
        this.sessionActivityGuard = sessionActivityGuard;
        this.stepUpGuard = stepUpGuard;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        UserWithRoles found = null;
        try {
            found = userQueryService.getByUsername(request.username());
        } catch (NotFoundException e) {
            // Fall through with found == null - handled below, after paying the same
            // BCrypt cost a real user lookup would have paid (see dummyPasswordHash).
        }

        // US-SEC-09 exception AC: a locked account gets a distinct refusal even before
        // checking the password - unlike username existence, the AC wants lockout state
        // surfaced, not hidden, so there's no reason to pay BCrypt's cost first here.
        if (found != null && lockoutService.isLocked(found.user())) {
            throw new AccountLockedException(found.user().getLockedUntil());
        }

        boolean passwordMatches = passwordEncoder.matches(
                request.password(), found != null ? found.user().getPasswordHash() : dummyPasswordHash);
        if (found == null || !passwordMatches || found.user().getStatus() != UserStatus.ACTIVE) {
            // Only a wrong password against a real account counts against the lockout
            // counter - a correct password against a deactivated account doesn't (see
            // UserLockoutService).
            if (found != null && !passwordMatches) {
                lockoutService.recordFailedAttempt(found.user());
            }
            securityEventLogger.record(SecurityEventType.LOGIN_FAILURE,
                    found != null ? found.user().getId() : null, request.username(), ip, null);
            // Same response, and now the same timing, whether the username doesn't exist or
            // the password is wrong (AC-SEC-04-X: the API never leaks whether a given
            // username exists).
            throw new InvalidCredentialsException();
        }

        lockoutService.recordSuccessfulLogin(found.user());
        securityEventLogger.record(SecurityEventType.LOGIN_SUCCESS, found.user().getId(), request.username(), ip, null);

        AppUser user = found.user();
        Set<String> roleCodes = roleCodesOf(found);
        Set<String> permissions = permissionsOf(found);
        CurrentUser currentUser = new CurrentUser(user.getId(), user.getUsername(), roleCodes, permissions);
        String token = jwtService.issue(currentUser);
        String refreshToken = refreshTokenService.issue(user.getId());
        sessionActivityGuard.start(user.getId()); // US-SEC-06: begin the idle clock for this fresh session
        return new LoginResponse(token, refreshToken, "Bearer", jwtService.expirationSeconds());
    }

    /** US-SEC-01-H (exchange) + US-SEC-06 (idle timeout enforced here - see RefreshTokenService.rotate). */
    @PostMapping("/refresh")
    public LoginResponse refresh(@Valid @RequestBody RefreshRequest request) {
        RefreshTokenService.Rotated rotated = refreshTokenService.rotate(request.refreshToken());

        UserWithRoles found;
        try {
            found = userQueryService.get(rotated.userId());
        } catch (NotFoundException e) {
            throw new InvalidRefreshTokenException();
        }
        // A deactivated or currently-locked account doesn't get a fresh access token just
        // because it still holds an unexpired refresh token - the same gate login() applies.
        if (found.user().getStatus() != UserStatus.ACTIVE || lockoutService.isLocked(found.user())) {
            throw new InvalidRefreshTokenException();
        }

        CurrentUser currentUser = new CurrentUser(found.user().getId(), found.user().getUsername(),
                roleCodesOf(found), permissionsOf(found));
        String accessToken = jwtService.issue(currentUser);
        sessionActivityGuard.start(found.user().getId()); // US-SEC-06: a refresh is activity - reset the idle clock
        return new LoginResponse(accessToken, rotated.rawToken(), "Bearer", jwtService.expirationSeconds());
    }

    /** US-SEC-01: logout - end exactly this session. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        refreshTokenService.revoke(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    /** US-USR-08 / US-SEC-01: end every session for the calling user. */
    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll() {
        UUID userId = currentUserProvider.current().id();
        refreshTokenService.revokeAll(userId);
        securityEventLogger.record(SecurityEventType.LOGOUT_ALL, userId, null, null, null);
        return ResponseEntity.noContent().build();
    }

    /**
     * US-SEC-09's self-service unlock, step 1. Always 202 - whether the
     * username exists or is even currently locked never leaks in the
     * response (see UserLockoutService.requestSelfServiceUnlock).
     */
    @PostMapping("/unlock/request")
    public ResponseEntity<Void> requestUnlock(@Valid @RequestBody UnlockRequestRequest request) {
        lockoutService.requestSelfServiceUnlock(request.username());
        return ResponseEntity.accepted().build();
    }

    /** US-SEC-09's self-service unlock, step 2: redeem the emailed code. */
    @PostMapping("/unlock/confirm")
    public ResponseEntity<Void> confirmUnlock(@Valid @RequestBody UnlockConfirmRequest request) {
        lockoutService.confirmSelfServiceUnlock(request.token());
        return ResponseEntity.noContent().build();
    }

    /**
     * US-SEC-06 (AC-SEC-06-X): re-confirm the caller's own password to satisfy
     * a step-up-required action's freshness window. Deliberately separate
     * from login's lockout counter - this is an already-authenticated caller
     * proving they're still at the keyboard, not a credential-guessing
     * surface, so a wrong entry here doesn't count toward account lockout.
     */
    @PostMapping("/step-up")
    public ResponseEntity<Void> stepUp(@Valid @RequestBody StepUpRequest request) {
        UUID userId = currentUserProvider.current().id();
        AppUser user = userQueryService.get(userId).user();
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        stepUpGuard.confirm(userId);
        securityEventLogger.record(SecurityEventType.STEP_UP_VERIFIED, userId, user.getUsername(), null, null);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public MeResponse me() {
        CurrentUser authenticated = currentUserProvider.current();
        UserWithRoles found = userQueryService.get(authenticated.id());
        AppUser user = found.user();

        Set<String> roleCodes = roleCodesOf(found);
        Set<String> permissions = permissionsOf(found);

        return new MeResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                roleCodes,
                user.getOrgScopeNode() != null ? user.getOrgScopeNode().getId() : null,
                permissions
        );
    }

    private Set<String> roleCodesOf(UserWithRoles found) {
        return found.roles().stream().map(Role::getCode).collect(Collectors.toSet());
    }

    private Set<String> permissionsOf(UserWithRoles found) {
        return found.roles().stream().flatMap(role -> role.getPermissions().stream()).collect(Collectors.toSet());
    }
}
