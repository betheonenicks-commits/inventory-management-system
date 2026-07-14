package com.iams.asset.application;

import com.iams.asset.domain.Asset;
import com.iams.asset.domain.AssetHistoryEventType;
import com.iams.asset.domain.AssetRepository;
import com.iams.common.exception.ConflictException;
import com.iams.common.exception.NotFoundException;
import com.iams.common.exception.ValidationFailedException;
import com.iams.common.security.CurrentUserProvider;
import com.iams.usr.application.OrgScopeGuard;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Parent-child (component) asset linking, US-AST-04. Deliberately a strict
 * two-level model - a child can't itself have children, and a parent can't
 * itself be a child - matching the "bundled equipment" scope in the epic's
 * acceptance criteria, not a general hierarchy. Deeper hierarchy is not a
 * requirement anywhere in the FRS; broadening this later is a data-model
 * change, not just a validation relaxation, so it's deliberately kept narrow.
 */
@Service
public class AssetHierarchyService {

    private final AssetRepository assetRepository;
    private final AssetHistoryRecorder historyRecorder;
    private final CurrentUserProvider currentUserProvider;
    private final OrgScopeGuard scopeGuard;

    public AssetHierarchyService(AssetRepository assetRepository,
                                  AssetHistoryRecorder historyRecorder,
                                  CurrentUserProvider currentUserProvider,
                                  OrgScopeGuard scopeGuard) {
        this.assetRepository = assetRepository;
        this.historyRecorder = historyRecorder;
        this.currentUserProvider = currentUserProvider;
        this.scopeGuard = scopeGuard;
    }

    @Transactional(readOnly = true)
    public List<Asset> listChildren(UUID parentId) {
        // FR-USR-04: fetching the parent (rather than existsById) lets us enforce scope on
        // it the same way AssetQueryService.get() does - a scoped actor who can't see the
        // parent asset directly shouldn't be able to see its children via this side door.
        Asset parent = assetRepository.findById(parentId).orElseThrow(() -> NotFoundException.of("Asset", parentId));
        UUID parentOrgNodeId = parent.getOrgNode() != null ? parent.getOrgNode().getId() : null;
        scopeGuard.requireWithinScope(parentOrgNodeId, "asset", parentId);

        List<Asset> children = assetRepository.findByParentAssetIdWithAssociationsOrderByCreatedAtAsc(parentId);
        // Defense in depth: filter children individually too, in case a child's own
        // orgNode ever diverges from its parent's (the data model doesn't force them to match).
        return scopeGuard.filterToScope(children, child -> child.getOrgNode() != null ? child.getOrgNode().getId() : null);
    }

    @Transactional
    public Asset linkChild(UUID parentId, UUID childId) {
        if (parentId.equals(childId)) {
            throw ValidationFailedException.singleField("childAssetId", "An asset cannot be its own component");
        }

        Asset parent = assetRepository.findById(parentId).orElseThrow(() -> NotFoundException.of("Asset", parentId));
        // child is returned and mapped to a response by the controller - needs its
        // associations fetched, unlike parent which is only read for validation here.
        Asset child = assetRepository.findByIdWithAssociations(childId).orElseThrow(() -> NotFoundException.of("Asset", childId));

        if (parent.getParentAsset() != null) {
            throw new ConflictException("ASSET_ALREADY_A_COMPONENT",
                    "Asset '" + parent.getAssetNumber() + "' is itself a component of another asset and cannot have components of its own.");
        }
        if (child.getParentAsset() != null) {
            throw new ConflictException("ASSET_ALREADY_A_COMPONENT",
                    "Asset '" + child.getAssetNumber() + "' already belongs to a parent asset. Unlink it first.");
        }
        if (assetRepository.existsByParentAssetId(childId)) {
            throw new ConflictException("ASSET_HAS_COMPONENTS",
                    "Asset '" + child.getAssetNumber() + "' already has its own components and cannot become a component.");
        }

        child.setParentAsset(parent);
        child.setUpdatedBy(currentUserProvider.current().id());
        child = assetRepository.saveAndFlush(child);

        historyRecorder.record(child, AssetHistoryEventType.FIELD_UPDATE, "parentAssetId", null, parent.getAssetNumber());

        return child;
    }

    @Transactional
    public void unlinkChild(UUID parentId, UUID childId) {
        Asset child = assetRepository.findById(childId).orElseThrow(() -> NotFoundException.of("Asset", childId));

        if (child.getParentAsset() == null || !child.getParentAsset().getId().equals(parentId)) {
            throw NotFoundException.of("Asset", childId);
        }

        String previousParentNumber = child.getParentAsset().getAssetNumber();
        child.setParentAsset(null);
        child.setUpdatedBy(currentUserProvider.current().id());
        assetRepository.saveAndFlush(child);

        historyRecorder.record(child, AssetHistoryEventType.FIELD_UPDATE, "parentAssetId", previousParentNumber, null);
    }
}
