package com.iams.maintenance.api;

import com.iams.maintenance.api.dto.RepairCloseRequest;
import com.iams.maintenance.api.dto.RepairEventResponse;
import com.iams.maintenance.api.dto.RepairOpenRequest;
import com.iams.maintenance.application.CloseRepairCommand;
import com.iams.maintenance.application.OpenRepairCommand;
import com.iams.maintenance.application.RepairService;
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

/** US-LIF-06: log an asset going out for repair and its return. */
@RestController
@RequestMapping("/api/v1/repairs")
public class RepairController {

    private final RepairService repairService;
    private final MaintenanceMapper mapper;

    public RepairController(RepairService repairService, MaintenanceMapper mapper) {
        this.repairService = repairService;
        this.mapper = mapper;
    }

    @PostMapping
    @PreAuthorize("@perm.has('assets:write')")
    public ResponseEntity<RepairEventResponse> open(@Valid @RequestBody RepairOpenRequest request) {
        var event = repairService.open(new OpenRepairCommand(request.assetId(), request.vendorName(), request.reason(),
                request.estimatedCost(), request.expectedReturnDate()));
        return ResponseEntity.created(URI.create("/api/v1/repairs/" + event.getId())).body(mapper.toResponse(event));
    }

    @GetMapping
    @PreAuthorize("@perm.has('assets:read')")
    public List<RepairEventResponse> list(@RequestParam(required = false) UUID assetId) {
        return repairService.list(assetId).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('assets:read')")
    public RepairEventResponse get(@PathVariable UUID id) {
        return mapper.toResponse(repairService.get(id));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("@perm.has('assets:write')")
    public RepairEventResponse close(@PathVariable UUID id, @Valid @RequestBody RepairCloseRequest request) {
        return mapper.toResponse(repairService.close(id, new CloseRepairCommand(request.actualReturnDate(), request.actualCost())));
    }
}
