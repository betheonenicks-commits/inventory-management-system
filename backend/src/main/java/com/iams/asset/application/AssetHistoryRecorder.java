package com.iams.asset.application;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetHistoryEvent;
import com.iams.asset.domain.AssetHistoryEventRepository;
import com.iams.asset.domain.AssetHistoryEventType;
import com.iams.common.security.CurrentUserProvider;
import org.springframework.stereotype.Component;

/**
 * The single place every AST-module history row gets written (FR-AST-10:
 * "every change becomes an immutable row" is structural because every
 * mutating service call flows through here, not duplicated per call site).
 */
@Component
public class AssetHistoryRecorder {

    private final AssetHistoryEventRepository historyRepository;
    private final CurrentUserProvider currentUserProvider;

    public AssetHistoryRecorder(AssetHistoryEventRepository historyRepository, CurrentUserProvider currentUserProvider) {
        this.historyRepository = historyRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public AssetHistoryEvent record(Asset asset, AssetHistoryEventType eventType, String fieldName, String oldValue, String newValue) {
        return record(asset, eventType, fieldName, oldValue, newValue, null);
    }

    /**
     * US-LIF-12: the restore-within-window overload - correctionOf links a new
     * event (e.g. a restore) back to the original event it's correcting/undoing
     * (e.g. the disposal), without editing that original row. Mirrors
     * AssetHistoryEvent.correctionOfEvent, reserved for exactly this since
     * FR-AST-10 but never previously written by any call site.
     */
    public AssetHistoryEvent record(Asset asset, AssetHistoryEventType eventType, String fieldName, String oldValue,
                                     String newValue, AssetHistoryEvent correctionOf) {
        AssetHistoryEvent event = new AssetHistoryEvent();
        event.setAsset(asset);
        event.setEventType(eventType);
        event.setFieldName(fieldName);
        event.setOldValue(oldValue);
        event.setNewValue(newValue);
        event.setCorrectionOfEvent(correctionOf);
        event.setCreatedBy(currentUserProvider.current().id());
        return historyRepository.save(event);
    }
}
