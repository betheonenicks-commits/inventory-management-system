package com.iams.maintenance.api;

import com.iams.maintenance.api.dto.MaintenanceScheduleCreateRequest;
import com.iams.maintenance.api.dto.MaintenanceScheduleResponse;
import com.iams.maintenance.application.MaintenanceScheduleService;
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

/** US-LIF-07: define a recurring preventive-maintenance cadence for an asset. */
@RestController
@RequestMapping("/api/v1/maintenance-schedules")
public class MaintenanceScheduleController {

    private final MaintenanceScheduleService scheduleService;
    private final MaintenanceMapper mapper;

    public MaintenanceScheduleController(MaintenanceScheduleService scheduleService, MaintenanceMapper mapper) {
        this.scheduleService = scheduleService;
        this.mapper = mapper;
    }

    @PostMapping
    @PreAuthorize("@perm.has('assets:write')")
    public ResponseEntity<MaintenanceScheduleResponse> create(@Valid @RequestBody MaintenanceScheduleCreateRequest request) {
        var schedule = scheduleService.create(request.assetId(), request.intervalMonths(), request.nextDueDate(), request.description());
        return ResponseEntity.created(URI.create("/api/v1/maintenance-schedules/" + schedule.getId())).body(mapper.toResponse(schedule));
    }

    @GetMapping
    @PreAuthorize("@perm.has('assets:read')")
    public List<MaintenanceScheduleResponse> list(@RequestParam(required = false) UUID assetId) {
        return scheduleService.list(assetId).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/due")
    @PreAuthorize("@perm.has('assets:read')")
    public List<MaintenanceScheduleResponse> due(@RequestParam(defaultValue = "30") int withinDays) {
        return scheduleService.dueWithin(withinDays).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("@perm.has('assets:read')")
    public MaintenanceScheduleResponse get(@PathVariable UUID id) {
        return mapper.toResponse(scheduleService.get(id));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("@perm.has('assets:write')")
    public MaintenanceScheduleResponse deactivate(@PathVariable UUID id) {
        return mapper.toResponse(scheduleService.deactivate(id));
    }
}
