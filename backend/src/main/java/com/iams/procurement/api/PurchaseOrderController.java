package com.iams.procurement.api;

import com.iams.lifecycle.api.dto.RejectRequest;
import com.iams.procurement.api.dto.PurchaseOrderCreateRequest;
import com.iams.procurement.api.dto.PurchaseOrderLineEventResponse;
import com.iams.procurement.api.dto.PurchaseOrderLineResponse;
import com.iams.procurement.api.dto.PurchaseOrderResponse;
import com.iams.procurement.api.dto.ReceiveLineRequest;
import com.iams.procurement.api.dto.ReturnToVendorRequest;
import com.iams.procurement.application.PurchaseOrderCreateCommand;
import com.iams.procurement.application.PurchaseOrderLineCommand;
import com.iams.procurement.application.PurchaseOrderService;
import com.iams.procurement.domain.PurchaseOrderStatus;
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

/** US-LIF-02/03/16: create a PO from an approved request, then receive/cancel/return-to-vendor its lines. */
@RestController
@RequestMapping("/api/v1/purchase-orders")
public class PurchaseOrderController {

    private final PurchaseOrderService orderService;
    private final ProcurementMapper mapper;

    public PurchaseOrderController(PurchaseOrderService orderService, ProcurementMapper mapper) {
        this.orderService = orderService;
        this.mapper = mapper;
    }

    @PostMapping
    @PreAuthorize("@perm.has('assets:write')")
    public ResponseEntity<PurchaseOrderResponse> create(@Valid @RequestBody PurchaseOrderCreateRequest request) {
        var lines = request.lines().stream()
                .map(l -> new PurchaseOrderLineCommand(l.description(), l.quantityOrdered(), l.unitCost()))
                .toList();
        var order = orderService.create(new PurchaseOrderCreateCommand(request.purchaseRequestId(), request.vendorName(), lines));
        return ResponseEntity.created(URI.create("/api/v1/purchase-orders/" + order.getId())).body(mapper.toResponse(order));
    }

    @GetMapping
    @PreAuthorize("@perm.has('assets:read')")
    public List<PurchaseOrderResponse> list(@RequestParam(required = false) PurchaseOrderStatus status) {
        return orderService.list(status).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('assets:read')")
    public PurchaseOrderResponse get(@PathVariable UUID id) {
        return mapper.toResponse(orderService.get(id));
    }

    @GetMapping("/{id}/lines")
    @PreAuthorize("@perm.has('assets:read')")
    public List<PurchaseOrderLineResponse> lines(@PathVariable UUID id) {
        return orderService.lines(id).stream().map(mapper::toResponse).toList();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@perm.has('assets:write')")
    public PurchaseOrderResponse cancel(@PathVariable UUID id, @Valid @RequestBody RejectRequest request) {
        return mapper.toResponse(orderService.cancel(id, request.reason()));
    }

    @GetMapping("/lines/{lineId}/events")
    @PreAuthorize("@perm.has('assets:read')")
    public List<PurchaseOrderLineEventResponse> lineEvents(@PathVariable UUID lineId) {
        return orderService.lineEvents(lineId).stream().map(mapper::toResponse).toList();
    }

    @PostMapping("/lines/{lineId}/receive")
    @PreAuthorize("@perm.has('assets:write')")
    public PurchaseOrderLineResponse receive(@PathVariable UUID lineId, @Valid @RequestBody ReceiveLineRequest request) {
        return mapper.toResponse(orderService.receive(lineId, request.quantity(), request.discrepancyNote()));
    }

    @PostMapping("/lines/{lineId}/return-to-vendor")
    @PreAuthorize("@perm.has('assets:write')")
    public PurchaseOrderLineResponse returnToVendor(@PathVariable UUID lineId, @Valid @RequestBody ReturnToVendorRequest request) {
        return mapper.toResponse(orderService.returnToVendor(lineId, request.quantity(), request.reason()));
    }
}
