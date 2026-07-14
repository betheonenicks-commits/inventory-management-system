package com.iams.maintenance.api;

import com.iams.maintenance.api.dto.MaintenanceEventResponse;
import com.iams.maintenance.api.dto.MaintenanceScheduleResponse;
import com.iams.maintenance.api.dto.RepairEventResponse;
import com.iams.maintenance.domain.MaintenanceEvent;
import com.iams.maintenance.domain.MaintenanceSchedule;
import com.iams.maintenance.domain.RepairEvent;
import org.springframework.stereotype.Component;

@Component
public class MaintenanceMapper {

    public RepairEventResponse toResponse(RepairEvent event) {
        return new RepairEventResponse(
                event.getId(),
                event.getVersion(),
                event.getAsset().getId(),
                event.getAsset().getAssetNumber(),
                event.getPreviousStatusCode(),
                event.getVendorName(),
                event.getReason(),
                event.getEstimatedCost(),
                event.getExpectedReturnDate(),
                event.getActualCost(),
                event.getActualReturnDate(),
                event.getStatus(),
                event.getLoggedBy()
        );
    }

    public MaintenanceScheduleResponse toResponse(MaintenanceSchedule schedule) {
        return new MaintenanceScheduleResponse(
                schedule.getId(),
                schedule.getVersion(),
                schedule.getAsset().getId(),
                schedule.getAsset().getAssetNumber(),
                schedule.getIntervalMonths(),
                schedule.getNextDueDate(),
                schedule.getDescription(),
                schedule.isActive()
        );
    }

    public MaintenanceEventResponse toResponse(MaintenanceEvent event) {
        return new MaintenanceEventResponse(
                event.getId(),
                event.getVersion(),
                event.getAsset().getId(),
                event.getAsset().getAssetNumber(),
                event.getSchedule() != null ? event.getSchedule().getId() : null,
                event.getMaintenanceType(),
                event.getPerformedAt(),
                event.getNotes(),
                event.getCost(),
                event.getPerformedBy()
        );
    }
}
