package com.iams.asset.application;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetHistoryEvent;
import com.iams.asset.domain.AssetHistoryEventRepository;
import com.iams.asset.domain.AssetHistoryEventType;
import com.iams.asset.domain.AssetRepository;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.org.domain.OrgNode;
import com.iams.org.domain.OrgNodeRepository;
import com.iams.usr.application.OrgScopeGuard;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    /**
     * US-SRC-03 (AC-SRC-03-X): the closed set of sortable columns. An
     * arbitrary property name would otherwise reach the Criteria API and
     * blow up as a path error deep in Hibernate - rejected up front instead.
     */
    private static final Set<String> SORTABLE = Set.of("assetNumber", "name", "createdAt", "purchaseDate");

    private final AssetRepository assetRepository;
    private final AssetHistoryEventRepository historyRepository;
    private final OrgScopeGuard scopeGuard;
    private final OrgNodeRepository orgNodeRepository;

    public AssetQueryService(AssetRepository assetRepository, AssetHistoryEventRepository historyRepository,
                              OrgScopeGuard scopeGuard, OrgNodeRepository orgNodeRepository) {
        this.assetRepository = assetRepository;
        this.historyRepository = historyRepository;
        this.scopeGuard = scopeGuard;
        this.orgNodeRepository = orgNodeRepository;
    }

    @Transactional(readOnly = true)
    public Asset get(UUID id) {
        Asset asset = assetRepository.findByIdWithAssociations(id).orElseThrow(() -> NotFoundException.of("Asset", id));
        UUID orgNodeId = asset.getOrgNode() != null ? asset.getOrgNode().getId() : null;
        scopeGuard.requireWithinScope(orgNodeId, "asset", id);
        return asset;
    }

    @Transactional(readOnly = true)
    public Page<Asset> list(UUID categoryId, UUID statusId, String query, UUID orgNodeId,
                             LocalDate purchasedFrom, LocalDate purchasedTo, Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!SORTABLE.contains(order.getProperty())) {
                throw ValidationFailedException.singleField("sort",
                        "Unsupported sort field '" + order.getProperty() + "' - supported: " + SORTABLE);
            }
        }
        if (purchasedFrom != null && purchasedTo != null && purchasedTo.isBefore(purchasedFrom)) {
            throw ValidationFailedException.singleField("purchasedTo", "Must not be before purchasedFrom");
        }
        String locationPrefix = null;
        if (orgNodeId != null) {
            // Same rule as the asset-register report: a requested node outside the
            // caller's own scope is refused, not silently emptied.
            scopeGuard.requireWithinScope(orgNodeId, "org node", orgNodeId);
            locationPrefix = orgNodeRepository.findById(orgNodeId).map(OrgNode::getPath)
                    .orElseThrow(() -> NotFoundException.of("OrgNode", orgNodeId));
        }
        return assetRepository.search(categoryId, statusId, query, locationPrefix,
                scopeGuard.currentScopePathPrefix(), purchasedFrom, purchasedTo, pageable);
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
