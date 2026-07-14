package com.iams.maintenance.api;

import com.iams.maintenance.api.dto.CorrectiveMaintenanceRecordRequest;
import com.iams.maintenance.api.dto.MaintenanceEventResponse;
import com.iams.maintenance.api.dto.PreventiveMaintenanceRecordRequest;
import com.iams.maintenance.application.MaintenanceEventService;
import com.iams.maintenance.domain.MaintenanceType;
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

/** US-LIF-07 (preventive completion) / US-LIF-08 (corrective, unscheduled). */
@RestController
@RequestMapping("/api/v1/maintenance-events")
public class MaintenanceEventController {

    private final MaintenanceEventService eventService;
    private final MaintenanceMapper mapper;

    public MaintenanceEventController(MaintenanceEventService eventService, MaintenanceMapper mapper) {
        this.eventService = eventService;
        this.mapper = mapper;
    }

    @PostMapping("/preventive")
    @PreAuthorize("@perm.has('assets:write')")
    public ResponseEntity<MaintenanceEventResponse> recordPreventive(@Valid @RequestBody PreventiveMaintenanceRecordRequest request) {
        var event = eventService.recordPreventive(request.scheduleId(), request.notes(), request.cost());
        return ResponseEntity.created(URI.create("/api/v1/maintenance-events/" + event.getId())).body(mapper.toResponse(event));
    }

    @PostMapping("/corrective")
    @PreAuthorize("@perm.has('assets:write')")
    public ResponseEntity<MaintenanceEventResponse> recordCorrective(@Valid @RequestBody CorrectiveMaintenanceRecordRequest request) {
        var event = eventService.recordCorrective(request.assetId(), request.notes(), request.cost());
        return ResponseEntity.created(URI.create("/api/v1/maintenance-events/" + event.getId())).body(mapper.toResponse(event));
    }

    @GetMapping
    @PreAuthorize("@perm.has('assets:read')")
    public List<MaintenanceEventResponse> list(@RequestParam(required = false) UUID assetId,
                                                @RequestParam(required = false) MaintenanceType maintenanceType) {
        return eventService.list(assetId, maintenanceType).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('assets:read')")
    public MaintenanceEventResponse get(@PathVariable UUID id) {
        return mapper.toResponse(eventService.get(id));
    }
}
