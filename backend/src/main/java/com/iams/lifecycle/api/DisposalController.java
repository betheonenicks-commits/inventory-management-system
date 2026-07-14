package com.iams.lifecycle.api;

import com.iams.lifecycle.api.dto.DisposalCreateRequest;
import com.iams.lifecycle.api.dto.DisposalResponse;
import com.iams.lifecycle.api.dto.RejectRequest;
import com.iams.lifecycle.application.DisposalCreateCommand;
import com.iams.lifecycle.application.DisposalService;
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

/** US-LIF-09/10/11/12/13: request, approve/reject/restore, and escalate a retirement/disposal/donation. */
@RestController
@RequestMapping("/api/v1/disposals")
public class DisposalController {

    private final DisposalService disposalService;
    private final LifecycleMapper mapper;

    public DisposalController(DisposalService disposalService, LifecycleMapper mapper) {
        this.disposalService = disposalService;
        this.mapper = mapper;
    }

    @PostMapping
    @PreAuthorize("@perm.has('assets:write')")
    public ResponseEntity<DisposalResponse> create(@Valid @RequestBody DisposalCreateRequest request) {
        var created = disposalService.create(new DisposalCreateCommand(request.assetId(), request.disposalType(),
                request.reason(), request.nominalApproverId()));
        return ResponseEntity.created(URI.create("/api/v1/disposals/" + created.getId())).body(mapper.toResponse(created));
    }

    @GetMapping
    @PreAuthorize("@perm.has('assets:read')")
    public List<DisposalResponse> list(@RequestParam(required = false) UUID assetId,
                                        @RequestParam(required = false) LifecycleRequestStatus status) {
        return disposalService.list(assetId, status).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('assets:read')")
    public DisposalResponse get(@PathVariable UUID id) {
        return mapper.toResponse(disposalService.get(id));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("@perm.has('approvals:write')")
    public DisposalResponse approve(@PathVariable UUID id) {
        return mapper.toResponse(disposalService.approve(id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("@perm.has('approvals:write')")
    public DisposalResponse reject(@PathVariable UUID id, @Valid @RequestBody RejectRequest request) {
        return mapper.toResponse(disposalService.reject(id, request.reason()));
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("@perm.has('assets:restore')")
    public DisposalResponse restore(@PathVariable UUID id) {
        return mapper.toResponse(disposalService.restore(id));
    }

    @PostMapping("/{id}/escalate")
    @PreAuthorize("@perm.has('approvals:write')")
    public DisposalResponse escalate(@PathVariable UUID id) {
        return mapper.toResponse(disposalService.escalate(id));
    }
}
