package com.iams.lifecycle.api;

import com.iams.lifecycle.api.dto.RejectRequest;
import com.iams.lifecycle.api.dto.TransferCreateRequest;
import com.iams.lifecycle.api.dto.TransferResponse;
import com.iams.lifecycle.application.TransferCreateCommand;
import com.iams.lifecycle.application.TransferService;
import com.iams.lifecycle.domain.LifecycleRequestStatus;
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
 * US-LIF-05/10/11/13: request, approve/reject, and escalate an asset transfer
 * between org nodes and/or custodians. Create is gated the same as day-to-day
 * asset writes (assets:write, Inventory Manager's existing permission);
 * approve/reject/escalate are gated approvals:write (Department Head's
 * existing permission) - the service layer's requireIsRoutedApprover is what
 * actually restricts action to *this* request's routed approver, the
 * permission check is only the coarse read/write gate.
 */
@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final TransferService transferService;
    private final LifecycleMapper mapper;

    public TransferController(TransferService transferService, LifecycleMapper mapper) {
        this.transferService = transferService;
        this.mapper = mapper;
    }

    @PostMapping
    @PreAuthorize("@perm.has('assets:write')")
    public ResponseEntity<TransferResponse> create(@Valid @RequestBody TransferCreateRequest request) {
        var created = transferService.create(new TransferCreateCommand(request.assetId(), request.toOrgNodeId(),
                request.toPersonId(), request.reason(), request.nominalApproverId(), request.childDispositions()));
        return ResponseEntity.created(URI.create("/api/v1/transfers/" + created.getId())).body(mapper.toResponse(created));
    }

    @GetMapping
    @PreAuthorize("@perm.has('assets:read')")
    public List<TransferResponse> list(@RequestParam(required = false) UUID assetId,
                                        @RequestParam(required = false) LifecycleRequestStatus status) {
        return transferService.list(assetId, status).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('assets:read')")
    public TransferResponse get(@PathVariable UUID id) {
        return mapper.toResponse(transferService.get(id));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("@perm.has('approvals:write')")
    public TransferResponse approve(@PathVariable UUID id) {
        return mapper.toResponse(transferService.approve(id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("@perm.has('approvals:write')")
    public TransferResponse reject(@PathVariable UUID id, @Valid @RequestBody RejectRequest request) {
        return mapper.toResponse(transferService.reject(id, request.reason()));
    }

    @PostMapping("/{id}/escalate")
    @PreAuthorize("@perm.has('approvals:write')")
    public TransferResponse escalate(@PathVariable UUID id) {
        return mapper.toResponse(transferService.escalate(id));
    }
}
