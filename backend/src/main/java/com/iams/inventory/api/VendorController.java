package com.iams.inventory.api;

import com.iams.inventory.api.dto.VendorCreateRequest;
import com.iams.inventory.api.dto.VendorResponse;
import com.iams.inventory.application.VendorService;
import com.iams.inventory.domain.Vendor;
import com.iams.procurement.api.ProcurementMapper;
import com.iams.procurement.api.dto.PurchaseOrderResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** US-INV-07/08: vendor CRUD, independent of items, plus their full purchase-order history. */
@RestController
@RequestMapping("/api/v1/vendors")
public class VendorController {

    private final VendorService vendorService;
    private final InventoryMapper mapper;
    private final ProcurementMapper procurementMapper;

    public VendorController(VendorService vendorService, InventoryMapper mapper, ProcurementMapper procurementMapper) {
        this.vendorService = vendorService;
        this.mapper = mapper;
        this.procurementMapper = procurementMapper;
    }

    @PostMapping
    @PreAuthorize("@perm.has('inventory:write')")
    public ResponseEntity<VendorResponse> create(@Valid @RequestBody VendorCreateRequest request) {
        Vendor vendor = vendorService.create(request.name(), request.contactEmail(), request.contactPhone());
        return ResponseEntity.created(URI.create("/api/v1/vendors/" + vendor.getId())).body(mapper.toResponse(vendor));
    }

    @GetMapping
    @PreAuthorize("@perm.has('inventory:read')")
    public List<VendorResponse> list() {
        return vendorService.list().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('inventory:read')")
    public VendorResponse get(@PathVariable UUID id) {
        return mapper.toResponse(vendorService.get(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@perm.has('inventory:write')")
    public VendorResponse update(@PathVariable UUID id, @Valid @RequestBody VendorCreateRequest request) {
        return mapper.toResponse(vendorService.update(id, request.name(), request.contactEmail(), request.contactPhone()));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("@perm.has('inventory:write')")
    public VendorResponse deactivate(@PathVariable UUID id) {
        return mapper.toResponse(vendorService.deactivate(id));
    }

    /** US-INV-07: "all 12 are listed with date, item, quantity, and cost" - the lines are fetched separately per PO, same as everywhere else POs are shown. */
    @GetMapping("/{id}/purchase-orders")
    @PreAuthorize("@perm.has('inventory:read')")
    public List<PurchaseOrderResponse> purchaseHistory(@PathVariable UUID id) {
        return vendorService.purchaseHistory(id).stream().map(procurementMapper::toResponse).toList();
    }
}
