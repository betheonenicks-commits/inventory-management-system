package com.iams.procurement.api;

import com.iams.lifecycle.api.dto.RejectRequest;
import com.iams.lifecycle.domain.LifecycleRequestStatus;
import com.iams.procurement.api.dto.PurchaseRequestCreateRequest;
import com.iams.procurement.api.dto.PurchaseRequestResponse;
import com.iams.procurement.application.PurchaseRequestCreateCommand;
import com.iams.procurement.application.PurchaseRequestService;
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

/** US-LIF-01: submit and approve/reject a purchase request. */
@RestController
@RequestMapping("/api/v1/purchase-requests")
public class PurchaseRequestController {

    private final PurchaseRequestService requestService;
    private final ProcurementMapper mapper;

    public PurchaseRequestController(PurchaseRequestService requestService, ProcurementMapper mapper) {
        this.requestService = requestService;
        this.mapper = mapper;
    }

    @PostMapping
    @PreAuthorize("@perm.has('assets:write')")
    public ResponseEntity<PurchaseRequestResponse> create(@Valid @RequestBody PurchaseRequestCreateRequest request) {
        var created = requestService.create(new PurchaseRequestCreateCommand(request.itemDescription(), request.justification(),
                request.estimatedCost(), request.vendorName(), request.nominalApproverId()));
        return ResponseEntity.created(URI.create("/api/v1/purchase-requests/" + created.getId())).body(mapper.toResponse(created));
    }

    @GetMapping
    @PreAuthorize("@perm.has('assets:read')")
    public List<PurchaseRequestResponse> list(@RequestParam(required = false) LifecycleRequestStatus status) {
        return requestService.list(status).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('assets:read')")
    public PurchaseRequestResponse get(@PathVariable UUID id) {
        return mapper.toResponse(requestService.get(id));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("@perm.has('approvals:write')")
    public PurchaseRequestResponse approve(@PathVariable UUID id) {
        return mapper.toResponse(requestService.approve(id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("@perm.has('approvals:write')")
    public PurchaseRequestResponse reject(@PathVariable UUID id, @Valid @RequestBody RejectRequest request) {
        return mapper.toResponse(requestService.reject(id, request.reason()));
    }
}
