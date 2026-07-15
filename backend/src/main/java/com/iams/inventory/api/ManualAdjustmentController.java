package com.iams.inventory.api;

import com.iams.inventory.api.dto.ManualAdjustmentCreateRequest;
import com.iams.inventory.api.dto.ManualAdjustmentResponse;
import com.iams.inventory.application.ManualAdjustmentService;
import com.iams.inventory.domain.InventoryManualAdjustmentRequest;
import com.iams.lifecycle.api.dto.RejectRequest;
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

/** US-INV-05: a manual quantity correction, requiring a reason and routed approval before it takes effect. */
@RestController
@RequestMapping("/api/v1/inventory-adjustments")
public class ManualAdjustmentController {

    private final ManualAdjustmentService adjustmentService;
    private final InventoryMapper mapper;

    public ManualAdjustmentController(ManualAdjustmentService adjustmentService, InventoryMapper mapper) {
        this.adjustmentService = adjustmentService;
        this.mapper = mapper;
    }

    @PostMapping
    @PreAuthorize("@perm.has('inventory:write')")
    public ResponseEntity<ManualAdjustmentResponse> create(@Valid @RequestBody ManualAdjustmentCreateRequest request) {
        InventoryManualAdjustmentRequest adjustment = adjustmentService.request(request.itemId(), request.warehouseId(),
                request.subLocation(), request.lotNumber(), request.quantityDelta(), request.reason(), request.nominalApproverId());
        return ResponseEntity.created(URI.create("/api/v1/inventory-adjustments/" + adjustment.getId())).body(mapper.toResponse(adjustment));
    }

    /**
     * inventory:read (Inventory Manager, sees everything) OR approvals:read (Department Head,
     * sees what's routed to them) - Department Head holds approvals:write to actually decide on
     * a request but never held inventory:read, so gating this on inventory:read alone would let
     * them call approve()/reject() blindly without ever being able to list or review the request
     * first. Same "gated on the wrong permission" class fixed twice already this session in
     * EPIC-AUD/EPIC-CMP - caught here by inspection before the frontend was built on top of it,
     * rather than by a live 403 after the fact.
     */
    @GetMapping
    @PreAuthorize("@perm.has('inventory:read') or @perm.has('approvals:read')")
    public List<ManualAdjustmentResponse> list(@RequestParam(required = false) LifecycleRequestStatus status) {
        return adjustmentService.list(status).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('inventory:read') or @perm.has('approvals:read')")
    public ManualAdjustmentResponse get(@PathVariable UUID id) {
        return mapper.toResponse(adjustmentService.get(id));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("@perm.has('approvals:write')")
    public ManualAdjustmentResponse approve(@PathVariable UUID id) {
        return mapper.toResponse(adjustmentService.approve(id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("@perm.has('approvals:write')")
    public ManualAdjustmentResponse reject(@PathVariable UUID id, @Valid @RequestBody RejectRequest request) {
        return mapper.toResponse(adjustmentService.reject(id, request.reason()));
    }
}
