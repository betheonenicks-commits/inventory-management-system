package com.iams.usr.api;

import com.iams.common.security.CurrentUserProvider;
import com.iams.usr.api.dto.UserCreateRequest;
import com.iams.usr.api.dto.UserDeactivateRequest;
import com.iams.usr.api.dto.UserResponse;
import com.iams.usr.api.dto.UserSummaryResponse;
import com.iams.usr.application.UserDeactivationService;
import com.iams.usr.application.UserLockoutService;
import com.iams.usr.application.UserProvisioningService;
import com.iams.usr.application.UserQueryService;
import com.iams.usr.domain.UserStatus;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * US-USR-01 (provision), US-USR-08 (block offboarding while assets remain
 * assigned). list()/get() are Administrator+ only - user accounts are not
 * something every role can browse. pickable() is the deliberate exception:
 * it's open to any authenticated user but returns only id/displayName.
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserProvisioningService provisioningService;
    private final UserDeactivationService deactivationService;
    private final UserLockoutService lockoutService;
    private final UserQueryService queryService;
    private final UserMapper mapper;
    private final CurrentUserProvider currentUserProvider;

    public UserController(UserProvisioningService provisioningService, UserDeactivationService deactivationService,
                           UserLockoutService lockoutService, UserQueryService queryService, UserMapper mapper,
                           CurrentUserProvider currentUserProvider) {
        this.provisioningService = provisioningService;
        this.deactivationService = deactivationService;
        this.lockoutService = lockoutService;
        this.queryService = queryService;
        this.mapper = mapper;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    @PreAuthorize("@perm.has('users:read')")
    public List<UserResponse> list() {
        return queryService.list().stream().map(mapper::toResponse).toList();
    }

    /**
     * Any authenticated user - not just users:read holders - needs to name a colleague
     * as a transfer/disposal/purchase-request approver (US-LIF-05/10, US-PRC-01), so this
     * intentionally has no @PreAuthorize, same as PersonController.list(). Returns only
     * id/displayName of active users, never the sensitive fields on UserResponse.
     */
    @GetMapping("/pickable")
    public List<UserSummaryResponse> pickable() {
        return queryService.list().stream()
                .filter(u -> u.user().getStatus() == UserStatus.ACTIVE)
                .map(mapper::toSummary)
                .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('users:read')")
    public UserResponse get(@PathVariable UUID id) {
        return mapper.toResponse(queryService.get(id));
    }

    @PostMapping
    @PreAuthorize("@perm.has('users:write')")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        var user = provisioningService.create(request.username(), request.password(), request.displayName(),
                request.email(), request.personId(), request.orgScopeNodeId(), request.roleCodes());
        UserResponse response = mapper.toResponse(queryService.get(user.getId()));
        return ResponseEntity.created(URI.create("/api/v1/users/" + user.getId())).body(response);
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("@perm.has('users:write')")
    public UserResponse deactivate(@PathVariable UUID id, @Valid @RequestBody UserDeactivateRequest request) {
        deactivationService.deactivate(id, request.version());
        return mapper.toResponse(queryService.get(id));
    }

    /** US-SEC-09 AC: admin unlock. Self-service unlock isn't built - see UserLockoutService. */
    @PostMapping("/{id}/unlock")
    @PreAuthorize("@perm.has('users:write')")
    public UserResponse unlock(@PathVariable UUID id) {
        lockoutService.unlock(id, currentUserProvider.current().id());
        return mapper.toResponse(queryService.get(id));
    }
}
