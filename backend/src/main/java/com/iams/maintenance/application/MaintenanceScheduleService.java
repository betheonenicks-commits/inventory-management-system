package com.iams.maintenance.application;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetRepository;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.maintenance.domain.MaintenanceSchedule;
import com.iams.maintenance.domain.MaintenanceScheduleRepository;
import com.iams.usr.application.OrgScopeGuard;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** US-LIF-07: define a recurring preventive-maintenance cadence for an asset. */
@Service
public class MaintenanceScheduleService {

    private final MaintenanceScheduleRepository scheduleRepository;
    private final AssetRepository assetRepository;
    private final CurrentUserProvider currentUserProvider;
    private final OrgScopeGuard scopeGuard;

    public MaintenanceScheduleService(MaintenanceScheduleRepository scheduleRepository, AssetRepository assetRepository,
                                       CurrentUserProvider currentUserProvider, OrgScopeGuard scopeGuard) {
        this.scheduleRepository = scheduleRepository;
        this.assetRepository = assetRepository;
        this.currentUserProvider = currentUserProvider;
        this.scopeGuard = scopeGuard;
    }

    @Transactional
    public MaintenanceSchedule create(UUID assetId, int intervalMonths, LocalDate initialDueDate, String description) {
        if (intervalMonths <= 0) {
            throw ValidationFailedException.singleField("intervalMonths", "Must be a positive number of months");
        }
        if (initialDueDate == null) {
            throw ValidationFailedException.singleField("nextDueDate", "This field is required");
        }
        Asset asset = assetRepository.findByIdWithAssociations(assetId).orElseThrow(() -> NotFoundException.of("Asset", assetId));
        scopeGuard.requireWithinScope(asset.getOrgNode().getId(), "asset", assetId);

        MaintenanceSchedule schedule = new MaintenanceSchedule();
        schedule.setAsset(asset);
        schedule.setIntervalMonths(intervalMonths);
        schedule.setNextDueDate(initialDueDate);
        schedule.setDescription(description);
        schedule.setActive(true);
        schedule.setCreatedBy(currentUserProvider.current().id());
        return scheduleRepository.save(schedule);
    }

    @Transactional
    public MaintenanceSchedule deactivate(UUID id) {
        MaintenanceSchedule schedule = get(id);
        schedule.setActive(false);
        schedule.setUpdatedBy(currentUserProvider.current().id());
        return scheduleRepository.saveAndFlush(schedule);
    }

    @Transactional(readOnly = true)
    public MaintenanceSchedule get(UUID id) {
        return scheduleRepository.findByIdWithAsset(id).orElseThrow(() -> NotFoundException.of("MaintenanceSchedule", id));
    }

    @Transactional(readOnly = true)
    public List<MaintenanceSchedule> list(UUID assetId) {
        if (assetId != null) {
            return scheduleRepository.findByAssetIdWithAssetOrderByNextDueDateAsc(assetId);
        }
        return scheduleRepository.findAllWithAssetOrderByNextDueDateAsc();
    }

    /** US-LIF-07 AC: "surfaces on the maintenance-due dashboard/report" - the query a dashboard/report would consume. */
    @Transactional(readOnly = true)
    public List<MaintenanceSchedule> dueWithin(int withinDays) {
        return scheduleRepository.findDueOnOrBefore(LocalDate.now().plusDays(withinDays));
    }
}
