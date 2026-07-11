package com.iams.asset.application;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetHistoryEventType;
import com.iams.asset.domain.AssetRepository;
import com.iams.asset.domain.AssetStatusDef;
import com.iams.asset.domain.AssetStatusDefRepository;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.OptimisticLockConflictException;
import com.iams.common.security.CurrentUserProvider;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Explicit status transitions (FR-AST-07, US-AST-07). Kept separate from the
 * general PATCH /assets/{id} so status changes always produce a
 * STATUS_CHANGE history event, never get silently bundled into a FIELD_UPDATE.
 */
@Service
public class AssetStatusService {

    private final AssetRepository assetRepository;
    private final AssetStatusDefRepository statusDefRepository;
    private final AssetHistoryRecorder historyRecorder;
    private final CurrentUserProvider currentUserProvider;

    public AssetStatusService(AssetRepository assetRepository,
                               AssetStatusDefRepository statusDefRepository,
                               AssetHistoryRecorder historyRecorder,
                               CurrentUserProvider currentUserProvider) {
        this.assetRepository = assetRepository;
        this.statusDefRepository = statusDefRepository;
        this.historyRecorder = historyRecorder;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<AssetStatusDef> availableStatuses() {
        return statusDefRepository.findAll();
    }

    @Transactional
    public Asset changeStatus(UUID assetId, UUID newStatusId, long expectedVersion) {
        Asset asset = assetRepository.findById(assetId).orElseThrow(() -> NotFoundException.of("Asset", assetId));

        if (asset.getVersion() != expectedVersion) {
            throw new OptimisticLockConflictException(expectedVersion, asset.getVersion(), asset);
        }

        AssetStatusDef newStatus = statusDefRepository.findById(newStatusId)
                .orElseThrow(() -> NotFoundException.of("AssetStatusDef", newStatusId));
        AssetStatusDef oldStatus = asset.getStatus();

        if (oldStatus.getId().equals(newStatus.getId())) {
            return asset; // no-op transition, nothing to record
        }

        asset.setStatus(newStatus);
        asset.setUpdatedBy(currentUserProvider.current().id());

        try {
            asset = assetRepository.saveAndFlush(asset);
        } catch (OptimisticLockingFailureException e) {
            Asset current = assetRepository.findById(assetId).orElseThrow(() -> NotFoundException.of("Asset", assetId));
            throw new OptimisticLockConflictException(expectedVersion, current.getVersion(), current);
        }

        historyRecorder.record(asset, AssetHistoryEventType.STATUS_CHANGE, "status", oldStatus.getCode(), newStatus.getCode());
        return asset;
    }
}
