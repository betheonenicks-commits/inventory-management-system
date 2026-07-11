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

    public void record(Asset asset, AssetHistoryEventType eventType, String fieldName, String oldValue, String newValue) {
        AssetHistoryEvent event = new AssetHistoryEvent();
        event.setAsset(asset);
        event.setEventType(eventType);
        event.setFieldName(fieldName);
        event.setOldValue(oldValue);
        event.setNewValue(newValue);
        event.setCreatedBy(currentUserProvider.current().id());
        historyRepository.save(event);
    }
}
