package com.iams.asset.application;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetHistoryEvent;
import com.iams.asset.domain.AssetHistoryEventRepository;
import com.iams.asset.domain.AssetRepository;
import com.iams.common.exception.NotFoundException;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssetQueryService {

    private final AssetRepository assetRepository;
    private final AssetHistoryEventRepository historyRepository;

    public AssetQueryService(AssetRepository assetRepository, AssetHistoryEventRepository historyRepository) {
        this.assetRepository = assetRepository;
        this.historyRepository = historyRepository;
    }

    @Transactional(readOnly = true)
    public Asset get(UUID id) {
        return assetRepository.findById(id).orElseThrow(() -> NotFoundException.of("Asset", id));
    }

    @Transactional(readOnly = true)
    public Page<Asset> list(UUID categoryId, UUID statusId, String query, Pageable pageable) {
        return assetRepository.search(categoryId, statusId, query, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AssetHistoryEvent> history(UUID assetId, Pageable pageable) {
        // Confirm the asset exists so a bad id surfaces as 404, not an empty page.
        if (!assetRepository.existsById(assetId)) {
            throw NotFoundException.of("Asset", assetId);
        }
        return historyRepository.findByAssetIdOrderByCreatedAtDesc(assetId, pageable);
    }
}
