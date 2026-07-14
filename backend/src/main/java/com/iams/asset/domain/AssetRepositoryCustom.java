package com.iams.asset.domain;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Filtered/free-text search behind GET /assets - implemented with the
 * JPA Criteria API in AssetRepositoryCustomImpl since Spring Data derived
 * queries can't express "any of these optional filters, plus free text".
 */
public interface AssetRepositoryCustom {

    /**
     * scopePathPrefix, when non-null, restricts results to assets whose
     * orgNode.path starts with it - the scope node itself or any descendant
     * (FR-USR-04). Null means unrestricted - callers resolve that via
     * OrgScopeGuard.currentScopePathPrefix(), not here.
     */
    Page<Asset> search(UUID categoryId, UUID statusId, String query, String scopePathPrefix, Pageable pageable);
}
