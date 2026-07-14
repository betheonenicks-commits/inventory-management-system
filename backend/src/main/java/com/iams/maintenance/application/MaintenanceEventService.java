package com.iams.maintenance.application;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetRepository;
import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.maintenance.domain.MaintenanceEvent;
import com.iams.maintenance.domain.MaintenanceEventRepository;
import com.iams.maintenance.domain.MaintenanceSchedule;
import com.iams.maintenance.domain.MaintenanceScheduleRepository;
import com.iams.maintenance.domain.MaintenanceType;
import com.iams.usr.application.OrgScopeGuard;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-LIF-07 (recordPreventive - completion against a schedule, recalculates
 * the schedule's next due date) and US-LIF-08 (recordCorrective - unscheduled,
 * no schedule link, requires a root-cause note).
 */
@Service
public class MaintenanceEventService {

    private final MaintenanceEventRepository eventRepository;
    private final MaintenanceScheduleRepository scheduleRepository;
    private final AssetRepository assetRepository;
    private final CurrentUserProvider currentUserProvider;
    private final OrgScopeGuard scopeGuard;

    public MaintenanceEventService(MaintenanceEventRepository eventRepository, MaintenanceScheduleRepository scheduleRepository,
                                    AssetRepository assetRepository, CurrentUserProvider currentUserProvider,
                                    OrgScopeGuard scopeGuard) {
        this.eventRepository = eventRepository;
        this.scheduleRepository = scheduleRepository;
        this.assetRepository = assetRepository;
        this.currentUserProvider = currentUserProvider;
        this.scopeGuard = scopeGuard;
    }

    @Transactional
    public MaintenanceEvent recordPreventive(UUID scheduleId, String notes, BigDecimal cost) {
        MaintenanceSchedule schedule = scheduleRepository.findByIdWithAsset(scheduleId)
                .orElseThrow(() -> NotFoundException.of("MaintenanceSchedule", scheduleId));
        if (!schedule.isActive()) {
            throw new ConflictException("SCHEDULE_INACTIVE", "This maintenance schedule is no longer active");
        }
        scopeGuard.requireWithinScope(schedule.getAsset().getOrgNode().getId(), "asset", schedule.getAsset().getId());

        UUID actor = currentUserProvider.current().id();
        MaintenanceEvent event = new MaintenanceEvent();
        event.setAsset(schedule.getAsset());
        event.setSchedule(schedule);
        event.setMaintenanceType(MaintenanceType.PREVENTIVE);
        event.setPerformedAt(Instant.now());
        event.setNotes(notes);
        event.setCost(cost);
        event.setPerformedBy(actor);
        event.setCreatedBy(actor);
        event = eventRepository.save(event);

        // AC-LIF-07-H: "the next due date recalculates from the schedule" - advances from its
        // own prior value, not from today, so the cadence doesn't drift with each completion.
        schedule.setNextDueDate(schedule.getNextDueDate().plusMonths(schedule.getIntervalMonths()));
        schedule.setUpdatedBy(actor);
        scheduleRepository.saveAndFlush(schedule);

        return event;
    }

    @Transactional
    public MaintenanceEvent recordCorrective(UUID assetId, String rootCauseNote, BigDecimal cost) {
        if (rootCauseNote == null || rootCauseNote.isBlank()) {
            throw ValidationFailedException.singleField("notes", "A root-cause note is required for corrective maintenance");
        }
        Asset asset = assetRepository.findByIdWithAssociations(assetId).orElseThrow(() -> NotFoundException.of("Asset", assetId));
        scopeGuard.requireWithinScope(asset.getOrgNode().getId(), "asset", assetId);

        UUID actor = currentUserProvider.current().id();
        MaintenanceEvent event = new MaintenanceEvent();
        event.setAsset(asset);
        event.setSchedule(null);
        event.setMaintenanceType(MaintenanceType.CORRECTIVE);
        event.setPerformedAt(Instant.now());
        event.setNotes(rootCauseNote);
        event.setCost(cost);
        event.setPerformedBy(actor);
        event.setCreatedBy(actor);
        return eventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public MaintenanceEvent get(UUID id) {
        return eventRepository.findByIdWithAssociations(id).orElseThrow(() -> NotFoundException.of("MaintenanceEvent", id));
    }

    @Transactional(readOnly = true)
    public List<MaintenanceEvent> list(UUID assetId, MaintenanceType type) {
        if (assetId != null) {
            return eventRepository.findByAssetIdWithAssociationsOrderByPerformedAtDesc(assetId);
        }
        if (type != null) {
            return eventRepository.findByMaintenanceTypeWithAssociationsOrderByPerformedAtDesc(type);
        }
        return eventRepository.findAllWithAssociationsOrderByPerformedAtDesc();
    }
}
