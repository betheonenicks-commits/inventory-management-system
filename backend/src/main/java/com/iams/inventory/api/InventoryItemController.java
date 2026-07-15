package com.iams.inventory.api;

import com.iams.inventory.api.dto.InventoryItemCreateRequest;
import com.iams.inventory.api.dto.InventoryItemResponse;
import com.iams.inventory.api.dto.InventoryItemUpdateRequest;
import com.iams.inventory.application.InventoryItemService;
import com.iams.inventory.domain.InventoryItem;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** US-INV-01/06/11: the item catalog. */
@RestController
@RequestMapping("/api/v1/inventory-items")
public class InventoryItemController {

    private final InventoryItemService itemService;
    private final InventoryMapper mapper;

    public InventoryItemController(InventoryItemService itemService, InventoryMapper mapper) {
        this.itemService = itemService;
        this.mapper = mapper;
    }

    @PostMapping
    @PreAuthorize("@perm.has('inventory:write')")
    public ResponseEntity<InventoryItemResponse> create(@Valid @RequestBody InventoryItemCreateRequest request) {
        InventoryItem item = itemService.create(request.name(), request.sku(), request.unitOfMeasure(),
                request.reorderLevel(), request.costingMethod());
        return ResponseEntity.created(URI.create("/api/v1/inventory-items/" + item.getId())).body(mapper.toResponse(item));
    }

    @GetMapping
    @PreAuthorize("@perm.has('inventory:read')")
    public List<InventoryItemResponse> list(@RequestParam(defaultValue = "false") boolean activeOnly) {
        return itemService.list(activeOnly).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('inventory:read')")
    public InventoryItemResponse get(@PathVariable UUID id) {
        return mapper.toResponse(itemService.get(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@perm.has('inventory:write')")
    public InventoryItemResponse update(@PathVariable UUID id, @Valid @RequestBody InventoryItemUpdateRequest request) {
        return mapper.toResponse(itemService.update(id, request.name(), request.reorderLevel(), request.costingMethod()));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("@perm.has('inventory:write')")
    public InventoryItemResponse deactivate(@PathVariable UUID id) {
        return mapper.toResponse(itemService.deactivate(id));
    }
}
