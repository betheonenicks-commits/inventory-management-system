package com.iams.maintenance.application;

import com.iams.asset.application.AssetHistoryRecorder;
import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetHistoryEventType;
import com.iams.asset.domain.AssetRepository;
import com.iams.asset.domain.AssetStatusDef;
import com.iams.asset.domain.AssetStatusDefRepository;
import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.maintenance.domain.RepairEvent;
import com.iams.maintenance.domain.RepairEventRepository;
import com.iams.maintenance.domain.RepairEventStatus;
import com.iams.usr.application.OrgScopeGuard;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-LIF-06: log an asset going out for repair and its return. Unlike
 * TransferService/DisposalService, there's no approval step - the story
 * describes a plain "log it" action, not a request-approve workflow.
 */
@Service
public class RepairService {

    private static final String UNDER_REPAIR_STATUS_CODE = "UNDER_REPAIR";

    private final RepairEventRepository repairEventRepository;
    private final AssetRepository assetRepository;
    private final AssetStatusDefRepository statusDefRepository;
    private final AssetHistoryRecorder historyRecorder;
    private final CurrentUserProvider currentUserProvider;
    private final OrgScopeGuard scopeGuard;

    public RepairService(RepairEventRepository repairEventRepository, AssetRepository assetRepository,
                          AssetStatusDefRepository statusDefRepository, AssetHistoryRecorder historyRecorder,
                          CurrentUserProvider currentUserProvider, OrgScopeGuard scopeGuard) {
        this.repairEventRepository = repairEventRepository;
        this.assetRepository = assetRepository;
        this.statusDefRepository = statusDefRepository;
        this.historyRecorder = historyRecorder;
        this.currentUserProvider = currentUserProvider;
        this.scopeGuard = scopeGuard;
    }

    @Transactional
    public RepairEvent open(OpenRepairCommand command) {
        if (command.reason() == null || command.reason().isBlank()) {
            throw ValidationFailedException.singleField("reason", "A reason is required to log a repair");
        }
        Asset asset = assetRepository.findByIdWithAssociations(command.assetId())
                .orElseThrow(() -> NotFoundException.of("Asset", command.assetId()));
        scopeGuard.requireWithinScope(asset.getOrgNode().getId(), "asset", asset.getId());

        AssetStatusDef previousStatus = asset.getStatus();
        AssetStatusDef underRepair = statusDefRepository.findByCode(UNDER_REPAIR_STATUS_CODE)
                .orElseThrow(() -> new IllegalStateException(UNDER_REPAIR_STATUS_CODE + " status missing from seed data"));

        UUID actor = currentUserProvider.current().id();
        RepairEvent event = new RepairEvent();
        event.setAsset(asset);
        event.setPreviousStatusCode(previousStatus.getCode());
        event.setVendorName(command.vendorName());
        event.setReason(command.reason());
        event.setEstimatedCost(command.estimatedCost());
        event.setExpectedReturnDate(command.expectedReturnDate());
        event.setStatus(RepairEventStatus.OPEN);
        event.setLoggedBy(actor);
        event.setCreatedBy(actor);
        event = repairEventRepository.save(event);

        asset.setStatus(underRepair);
        asset.setUpdatedBy(actor);
        assetRepository.saveAndFlush(asset);
        historyRecorder.record(asset, AssetHistoryEventType.STATUS_CHANGE, "status", previousStatus.getCode(), underRepair.getCode());

        return event;
    }

    @Transactional
    public RepairEvent close(UUID id, CloseRepairCommand command) {
        RepairEvent event = get(id);
        if (event.getStatus() != RepairEventStatus.OPEN) {
            throw new ConflictException("REPAIR_ALREADY_CLOSED", "This repair event has already been closed");
        }
        if (command.actualReturnDate() == null) {
            throw ValidationFailedException.singleField("actualReturnDate", "This field is required to close a repair");
        }

        Asset asset = event.getAsset();
        AssetStatusDef revertTo = statusDefRepository.findByCode(event.getPreviousStatusCode())
                .orElseThrow(() -> new IllegalStateException(event.getPreviousStatusCode() + " status missing from seed data"));
        String currentStatusCode = asset.getStatus().getCode();

        UUID actor = currentUserProvider.current().id();
        asset.setStatus(revertTo);
        asset.setUpdatedBy(actor);
        assetRepository.saveAndFlush(asset);
        historyRecorder.record(asset, AssetHistoryEventType.STATUS_CHANGE, "status", currentStatusCode, revertTo.getCode());

        event.setActualReturnDate(command.actualReturnDate());
        event.setActualCost(command.actualCost());
        event.setStatus(RepairEventStatus.CLOSED);
        event.setUpdatedBy(actor);
        return repairEventRepository.saveAndFlush(event);
    }

    @Transactional(readOnly = true)
    public RepairEvent get(UUID id) {
        return repairEventRepository.findByIdWithAsset(id).orElseThrow(() -> NotFoundException.of("RepairEvent", id));
    }

    @Transactional(readOnly = true)
    public List<RepairEvent> list(UUID assetId) {
        if (assetId != null) {
            return repairEventRepository.findByAssetIdWithAssetOrderByCreatedAtDesc(assetId);
        }
        return repairEventRepository.findAllWithAssetOrderByCreatedAtDesc();
    }
}
