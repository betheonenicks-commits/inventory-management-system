package com.iams.inventory.api;

import com.iams.inventory.api.dto.WarehouseCreateRequest;
import com.iams.inventory.api.dto.WarehouseResponse;
import com.iams.inventory.api.dto.WarehouseUpdateRequest;
import com.iams.inventory.application.WarehouseService;
import com.iams.inventory.domain.Warehouse;
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

/** US-INV-03: warehouses with shelf/bin sub-location precision (the sub-location itself lives on stock balances/transactions, not here). */
@RestController
@RequestMapping("/api/v1/warehouses")
public class WarehouseController {

    private final WarehouseService warehouseService;
    private final InventoryMapper mapper;

    public WarehouseController(WarehouseService warehouseService, InventoryMapper mapper) {
        this.warehouseService = warehouseService;
        this.mapper = mapper;
    }

    @PostMapping
    @PreAuthorize("@perm.has('inventory:write')")
    public ResponseEntity<WarehouseResponse> create(@Valid @RequestBody WarehouseCreateRequest request) {
        Warehouse warehouse = warehouseService.create(request.name(), request.code(), request.orgNodeId());
        return ResponseEntity.created(URI.create("/api/v1/warehouses/" + warehouse.getId())).body(mapper.toResponse(warehouse));
    }

    @GetMapping
    @PreAuthorize("@perm.has('inventory:read')")
    public List<WarehouseResponse> list() {
        return warehouseService.list().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('inventory:read')")
    public WarehouseResponse get(@PathVariable UUID id) {
        return mapper.toResponse(warehouseService.get(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@perm.has('inventory:write')")
    public WarehouseResponse update(@PathVariable UUID id, @Valid @RequestBody WarehouseUpdateRequest request) {
        return mapper.toResponse(warehouseService.update(id, request.name()));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("@perm.has('inventory:write')")
    public WarehouseResponse deactivate(@PathVariable UUID id) {
        return mapper.toResponse(warehouseService.deactivate(id));
    }
}
