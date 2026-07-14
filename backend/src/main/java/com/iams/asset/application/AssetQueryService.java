package com.iams.asset.application;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetHistoryEvent;
import com.iams.asset.domain.AssetHistoryEventRepository;
import com.iams.asset.domain.AssetHistoryEventType;
import com.iams.asset.domain.AssetRepository;
import com.iams.common.exception.NotFoundException;
import com.iams.usr.application.OrgScopeGuard;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-USR-04: every list and detail fetch here is filtered to the acting
 * user's org-scope node via OrgScopeGuard - the scope node itself or any
 * descendant, since EPIC-ORG's hierarchy (2026-07-13). A direct-by-id fetch
 * outside scope is refused (AC-USR-04-X), not merely omitted from a list.
 */
@Service
public class AssetQueryService {

    private final AssetRepository assetRepository;
    private final AssetHistoryEventRepository historyRepository;
    private final OrgScopeGuard scopeGuard;

    public AssetQueryService(AssetRepository assetRepository, AssetHistoryEventRepository historyRepository,
                              OrgScopeGuard scopeGuard) {
        this.assetRepository = assetRepository;
        this.historyRepository = historyRepository;
        this.scopeGuard = scopeGuard;
    }

    @Transactional(readOnly = true)
    public Asset get(UUID id) {
        Asset asset = assetRepository.findByIdWithAssociations(id).orElseThrow(() -> NotFoundException.of("Asset", id));
        UUID orgNodeId = asset.getOrgNode() != null ? asset.getOrgNode().getId() : null;
        scopeGuard.requireWithinScope(orgNodeId, "asset", id);
        return asset;
    }

    @Transactional(readOnly = true)
    public Page<Asset> list(UUID categoryId, UUID statusId, String query, Pageable pageable) {
        return assetRepository.search(categoryId, statusId, query, scopeGuard.currentScopePathPrefix(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<AssetHistoryEvent> history(UUID assetId, Pageable pageable) {
        // get() enforces scope and 404s a bad id, so route history/movements through
        // it rather than a separate existsById check that would skip the scope gate.
        get(assetId);
        return historyRepository.findByAssetIdOrderByCreatedAtDesc(assetId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AssetHistoryEvent> movements(UUID assetId, Pageable pageable) {
        get(assetId);
        return historyRepository.findByAssetIdAndEventTypeOrderByCreatedAtDesc(assetId, AssetHistoryEventType.LOCATION_CHANGE, pageable);
    }
}
