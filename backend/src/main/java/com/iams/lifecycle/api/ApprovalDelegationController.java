package com.iams.lifecycle.api;

import com.iams.lifecycle.api.dto.ApprovalDelegationCreateRequest;
import com.iams.lifecycle.api.dto.ApprovalDelegationResponse;
import com.iams.lifecycle.application.ApprovalRoutingService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * US-LIF-15: a Department Head (or anyone routed approvals) delegating their
 * approval authority to a named alternate for a defined window. Gated
 * approvals:write - the same permission that lets someone act as an approver
 * in the first place is what lets them delegate that authority away.
 */
@RestController
@RequestMapping("/api/v1/approval-delegations")
public class ApprovalDelegationController {

    private final ApprovalRoutingService routingService;
    private final LifecycleMapper mapper;

    public ApprovalDelegationController(ApprovalRoutingService routingService, LifecycleMapper mapper) {
        this.routingService = routingService;
        this.mapper = mapper;
    }

    @PostMapping
    @PreAuthorize("@perm.has('approvals:write')")
    public ResponseEntity<ApprovalDelegationResponse> create(@Valid @RequestBody ApprovalDelegationCreateRequest request) {
        var delegation = routingService.createDelegation(request.delegateUserId(), request.validFrom(), request.validTo(), request.reason());
        return ResponseEntity.created(URI.create("/api/v1/approval-delegations/" + delegation.getId())).body(mapper.toResponse(delegation));
    }

    @GetMapping
    @PreAuthorize("@perm.has('approvals:write')")
    public List<ApprovalDelegationResponse> list(@RequestParam UUID delegatorUserId) {
        return routingService.list(delegatorUserId).stream().map(mapper::toResponse).toList();
    }

    @PostMapping("/{id}/revoke")
    @PreAuthorize("@perm.has('approvals:write')")
    public ApprovalDelegationResponse revoke(@PathVariable UUID id) {
        return mapper.toResponse(routingService.revoke(id));
    }
}
